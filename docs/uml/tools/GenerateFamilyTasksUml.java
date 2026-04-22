import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Documentation-only UML generator for the Family Tasks project.
 *
 * It parses real Java source files with JavaParser and writes PlantUML class
 * diagrams. No diagram class list is hand-authored: classes and project-to-project
 * dependencies come from the AST and source references under app/src/main/java.
 */
public class GenerateFamilyTasksUml {

    private static final String ROOT_PACKAGE = "com.example.family_tasks_proj";
    private static final Path SOURCE_ROOT = Paths.get("app", "src", "main", "java");
    private static final Path OUTPUT_ROOT = Paths.get("docs", "uml");
    private static final Path PLANTUML_DIR = OUTPUT_ROOT.resolve("plantuml");
    private static final Path IMAGE_DIR = OUTPUT_ROOT.resolve("images");
    private static final Path PDF_DIR = OUTPUT_ROOT.resolve("pdf");

    private final Map<String, ClassInfo> classesByQualifiedName = new LinkedHashMap<>();
    private final Map<String, List<String>> qualifiedNamesBySimpleName = new HashMap<>();
    private final Set<Relation> relations = new LinkedHashSet<>();

    public static void main(String[] args) throws Exception {
        GenerateFamilyTasksUml generator = new GenerateFamilyTasksUml();
        generator.run();
    }

    private void run() throws IOException {
        Files.createDirectories(PLANTUML_DIR);
        Files.createDirectories(IMAGE_DIR);
        Files.createDirectories(PDF_DIR);

        List<Path> javaFiles = Files.walk(SOURCE_ROOT)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.toString().contains(ROOT_PACKAGE.replace(".", java.io.File.separator)))
                .sorted()
                .collect(Collectors.toList());

        List<ParsedUnit> units = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            CompilationUnit unit = StaticJavaParser.parse(javaFile);
            units.add(new ParsedUnit(javaFile, unit));
            collectTypes(javaFile, unit);
        }

        indexSimpleNames();
        for (ParsedUnit parsedUnit : units) {
            collectRelations(parsedUnit);
        }

        writeDiagram("family_tasks_full_overview", "Family Tasks - Full Code-Generated Class Overview",
                includeAllProjectClasses(), false, false, true);
        writeDiagram("family_tasks_screens", "Family Tasks - Screen Classes",
                includeScreens(), false, false, true);
        writeDiagram("family_tasks_models", "Family Tasks - Model Classes",
                includeByGroups(Set.of(Group.MODEL)), true, false, true);
        writeDiagram("family_tasks_adapters", "Family Tasks - Adapter Classes",
                includeAdapters(), false, false, true);
        writeDiagram("family_tasks_utils_firebase", "Family Tasks - Utils and Firebase Classes",
                includeUtilsAndUsers(), false, true, true);

        writeProjectBookSection();
        writeGenerationReport(javaFiles);
    }

    private void collectTypes(Path javaFile, CompilationUnit unit) {
        String packageName = unit.getPackageDeclaration()
                .map(pd -> pd.getName().asString())
                .orElse("");
        Map<String, String> imports = new HashMap<>();
        for (ImportDeclaration importDeclaration : unit.getImports()) {
            if (!importDeclaration.isAsterisk()) {
                String imported = importDeclaration.getNameAsString();
                String simple = imported.substring(imported.lastIndexOf('.') + 1);
                imports.put(simple, imported);
            }
        }

        for (TypeDeclaration<?> type : unit.getTypes()) {
            collectType(javaFile, packageName, imports, type, null);
        }
    }

    private void collectType(Path javaFile,
                             String packageName,
                             Map<String, String> imports,
                             TypeDeclaration<?> type,
                             String parentQualifiedName) {
        String simpleName = type.getNameAsString();
        String qualifiedName = parentQualifiedName == null
                ? packageName + "." + simpleName
                : parentQualifiedName + "." + simpleName;

        ClassInfo info = new ClassInfo();
        info.simpleName = simpleName;
        info.qualifiedName = qualifiedName;
        info.packageName = packageName;
        info.parentQualifiedName = parentQualifiedName;
        info.sourceFile = javaFile;
        info.imports = imports;
        info.node = type;
        info.kind = getKind(type);
        info.group = classify(info);

        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            for (ClassOrInterfaceType extendedType : declaration.getExtendedTypes()) {
                info.extendsTypes.add(extendedType.getNameAsString());
            }
            for (ClassOrInterfaceType implementedType : declaration.getImplementedTypes()) {
                info.implementsTypes.add(implementedType.getNameAsString());
            }
        }

        for (FieldDeclaration field : type.getFields()) {
            for (VariableDeclarator variable : field.getVariables()) {
                String modifier = field.isPublic() ? "+" : field.isPrivate() ? "-" : field.isProtected() ? "#" : "~";
                info.fields.add(modifier + " " + variable.getNameAsString() + " : "
                        + simplifyType(variable.getType().asString()));
            }
        }

        classesByQualifiedName.put(qualifiedName, info);

        for (TypeDeclaration<?> childType : type.getMembers().stream()
                .filter(member -> member instanceof TypeDeclaration<?>)
                .map(member -> (TypeDeclaration<?>) member)
                .collect(Collectors.toList())) {
            collectType(javaFile, packageName, imports, childType, qualifiedName);
        }
    }

    private String getKind(TypeDeclaration<?> type) {
        if (type instanceof EnumDeclaration) return "enum";
        if (type instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface()) return "interface";
        return "class";
    }

    private Group classify(ClassInfo info) {
        if (info.parentQualifiedName != null) {
            String simple = info.simpleName;
            if (simple.endsWith("Adapter") || simple.endsWith("ViewHolder") || simple.endsWith("Listener")) {
                return Group.ADAPTER;
            }
            return Group.INNER;
        }
        if (info.packageName.endsWith(".util") || info.simpleName.equals("FBsingleton")) return Group.UTIL;
        if (info.packageName.contains(".model") || info.simpleName.equals("ParentInFb")) return Group.MODEL;
        if (info.simpleName.endsWith("Adapter")) return Group.ADAPTER;
        if (info.simpleName.endsWith("Activity") || info.simpleName.endsWith("Fragment")) return Group.SCREEN;
        return Group.INNER;
    }

    private void indexSimpleNames() {
        for (ClassInfo info : classesByQualifiedName.values()) {
            qualifiedNamesBySimpleName
                    .computeIfAbsent(info.simpleName, ignored -> new ArrayList<>())
                    .add(info.qualifiedName);
        }
    }

    private void collectRelations(ParsedUnit parsedUnit) {
        for (TypeDeclaration<?> topLevel : parsedUnit.unit.getTypes()) {
            collectRelationsForType(topLevel, null);
        }
    }

    private void collectRelationsForType(TypeDeclaration<?> type, String parentQualifiedName) {
        String packageName = type.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pd -> pd.getName().asString())
                .orElse("");
        String qualifiedName = parentQualifiedName == null
                ? packageName + "." + type.getNameAsString()
                : parentQualifiedName + "." + type.getNameAsString();

        ClassInfo source = classesByQualifiedName.get(qualifiedName);
        if (source == null) return;

        if (source.parentQualifiedName != null) {
            addRelation(source.parentQualifiedName, source.qualifiedName, RelationType.CONTAINS);
        }

        for (String extendedType : source.extendsTypes) {
            resolveType(source, extendedType).ifPresent(target ->
                    addRelation(target, source.qualifiedName, RelationType.EXTENDS));
        }
        for (String implementedType : source.implementsTypes) {
            resolveType(source, implementedType).ifPresent(target ->
                    addRelation(target, source.qualifiedName, RelationType.IMPLEMENTS));
        }

        for (ClassOrInterfaceType referencedType : type.findAll(ClassOrInterfaceType.class)) {
            resolveType(source, referencedType.getNameAsString()).ifPresent(target ->
                    addRelation(source.qualifiedName, target, RelationType.USES));
        }

        for (ClassExpr classExpr : type.findAll(ClassExpr.class)) {
            resolveType(source, classExpr.getType().asString()).ifPresent(target ->
                    addRelation(source.qualifiedName, target, RelationType.USES));
        }

        for (NameExpr nameExpr : type.findAll(NameExpr.class)) {
            resolveType(source, nameExpr.getNameAsString()).ifPresent(target ->
                    addRelation(source.qualifiedName, target, RelationType.USES));
        }

        for (FieldAccessExpr fieldAccessExpr : type.findAll(FieldAccessExpr.class)) {
            String scope = fieldAccessExpr.getScope().toString();
            resolveType(source, scope).ifPresent(target ->
                    addRelation(source.qualifiedName, target, RelationType.USES));
        }

        for (MethodCallExpr methodCallExpr : type.findAll(MethodCallExpr.class)) {
            methodCallExpr.getScope().ifPresent(scope ->
                    resolveType(source, scope.toString()).ifPresent(target ->
                            addRelation(source.qualifiedName, target, RelationType.USES)));
        }

        for (TypeDeclaration<?> childType : type.getMembers().stream()
                .filter(member -> member instanceof TypeDeclaration<?>)
                .map(member -> (TypeDeclaration<?>) member)
                .collect(Collectors.toList())) {
            collectRelationsForType(childType, source.qualifiedName);
        }
    }

    private Optional<String> resolveType(ClassInfo source, String rawType) {
        String simpleName = rawType;
        int genericStart = simpleName.indexOf('<');
        if (genericStart >= 0) simpleName = simpleName.substring(0, genericStart);
        int dot = simpleName.lastIndexOf('.');
        if (dot >= 0) simpleName = simpleName.substring(dot + 1);
        simpleName = simpleName.replace("[]", "").trim();
        if (simpleName.isEmpty()) return Optional.empty();

        String directNested = source.qualifiedName + "." + simpleName;
        if (classesByQualifiedName.containsKey(directNested)) return Optional.of(directNested);

        if (source.parentQualifiedName != null) {
            String siblingNested = source.parentQualifiedName + "." + simpleName;
            if (classesByQualifiedName.containsKey(siblingNested)) return Optional.of(siblingNested);
        }

        String imported = source.imports.get(simpleName);
        if (imported != null && classesByQualifiedName.containsKey(imported)) return Optional.of(imported);

        List<String> matches = qualifiedNamesBySimpleName.getOrDefault(simpleName, Collections.emptyList());
        if (matches.size() == 1) return Optional.of(matches.get(0));

        return Optional.empty();
    }

    private void addRelation(String from, String to, RelationType type) {
        if (from == null || to == null || from.equals(to)) return;
        if (!classesByQualifiedName.containsKey(from) || !classesByQualifiedName.containsKey(to)) return;

        Relation candidate = new Relation(from, to, type);
        if (type == RelationType.USES) {
            for (Relation existing : relations) {
                if (existing.sameEndpoints(candidate) && existing.type != RelationType.USES) {
                    return;
                }
            }
        }
        relations.removeIf(existing -> existing.sameEndpoints(candidate)
                && existing.type == RelationType.USES
                && candidate.type != RelationType.USES);
        relations.add(candidate);
    }

    private Set<String> includeAllProjectClasses() {
        return new LinkedHashSet<>(classesByQualifiedName.keySet());
    }

    private Set<String> includeByGroups(Set<Group> groups) {
        return classesByQualifiedName.values().stream()
                .filter(info -> groups.contains(info.group))
                .map(info -> info.qualifiedName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> includeScreens() {
        Set<String> included = includeByGroups(Set.of(Group.SCREEN, Group.INNER));
        includeOneHopTargets(included, Set.of(Group.MODEL, Group.ADAPTER, Group.UTIL));
        return included;
    }

    private Set<String> includeAdapters() {
        Set<String> included = includeByGroups(Set.of(Group.ADAPTER));
        includeChildrenOfIncluded(included);
        includeOneHopTargets(included, Set.of(Group.MODEL, Group.UTIL));
        return included;
    }

    private Set<String> includeUtilsAndUsers() {
        Set<String> included = includeByGroups(Set.of(Group.UTIL));
        for (Relation relation : relations) {
            if (included.contains(relation.to) || included.contains(relation.from)) {
                included.add(relation.from);
                included.add(relation.to);
            }
        }
        return included;
    }

    private void includeOneHopTargets(Set<String> included, Set<Group> allowedGroups) {
        boolean changed;
        do {
            changed = false;
            Set<String> additions = new LinkedHashSet<>();
            for (Relation relation : relations) {
                if (included.contains(relation.from)) {
                    ClassInfo target = classesByQualifiedName.get(relation.to);
                    if (target != null && allowedGroups.contains(target.group)) additions.add(relation.to);
                }
                if (included.contains(relation.to) && relation.type == RelationType.CONTAINS) {
                    additions.add(relation.from);
                }
            }
            for (String addition : additions) {
                if (included.add(addition)) changed = true;
            }
        } while (changed);
    }

    private void includeChildrenOfIncluded(Set<String> included) {
        for (ClassInfo info : classesByQualifiedName.values()) {
            if (info.parentQualifiedName != null && included.contains(info.parentQualifiedName)) {
                included.add(info.qualifiedName);
            }
        }
    }

    private void writeDiagram(String fileBaseName,
                              String title,
                              Set<String> included,
                              boolean showFields,
                              boolean leftToRight,
                              boolean splitUnconnected) throws IOException {
        Set<Relation> diagramRelations = relations.stream()
                .filter(relation -> included.contains(relation.from) && included.contains(relation.to))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> unconnected = splitUnconnected ? findUnconnected(included, diagramRelations) : Set.of();

        StringBuilder puml = new StringBuilder();
        puml.append("@startuml\n");
        puml.append("' Auto-generated from Java source by docs/uml/tools/GenerateFamilyTasksUml.java\n");
        puml.append("!pragma layout smetana\n");
        puml.append("skinparam dpi 240\n");
        puml.append("skinparam shadowing false\n");
        puml.append("skinparam linetype ortho\n");
        puml.append("skinparam classAttributeIconSize 0\n");
        puml.append("skinparam defaultFontName Arial\n");
        puml.append("skinparam classFontSize 15\n");
        puml.append("skinparam packageFontSize 18\n");
        puml.append("skinparam ArrowFontSize 12\n");
        puml.append("skinparam classBackgroundColor<<screen>> #DCEEFF\n");
        puml.append("skinparam classBorderColor<<screen>> #2F6FA3\n");
        puml.append("skinparam classBackgroundColor<<model>> #E2F5E6\n");
        puml.append("skinparam classBorderColor<<model>> #3B8B4F\n");
        puml.append("skinparam classBackgroundColor<<adapter>> #FFF3BF\n");
        puml.append("skinparam classBorderColor<<adapter>> #B68A00\n");
        puml.append("skinparam classBackgroundColor<<util>> #EFEFEF\n");
        puml.append("skinparam classBorderColor<<util>> #777777\n");
        puml.append("skinparam classBackgroundColor<<inner>> #FFFFFF\n");
        puml.append("skinparam classBorderColor<<inner>> #666666\n");
        if (leftToRight) puml.append("left to right direction\n");
        puml.append("hide empty methods\n");
        puml.append("title ").append(title).append("\n");
        puml.append("caption Generated from app/src/main/java with JavaParser; rendered by PlantUML.\n\n");

        writeGroupPackage(puml, "Screens / Activities / Fragments", "#DCEEFF", included, unconnected, showFields, Group.SCREEN);
        writeGroupPackage(puml, "Models", "#E2F5E6", included, unconnected, showFields, Group.MODEL);
        writeGroupPackage(puml, "Adapters", "#FFF3BF", included, unconnected, showFields, Group.ADAPTER);
        writeGroupPackage(puml, "Utils / Firebase", "#EFEFEF", included, unconnected, showFields, Group.UTIL);
        writeGroupPackage(puml, "Inner / support classes", "#FFFFFF", included, unconnected, showFields, Group.INNER);

        if (!unconnected.isEmpty()) {
            puml.append("package \"Unconnected in this diagram\" #FFFFFF {\n");
            for (String qname : sorted(unconnected)) {
                writeClass(puml, classesByQualifiedName.get(qname), showFields);
            }
            puml.append("}\n\n");
        }

        for (Relation relation : diagramRelations) {
            if (unconnected.contains(relation.from) || unconnected.contains(relation.to)) continue;
            puml.append(formatRelation(relation)).append("\n");
        }

        puml.append("\nlegend right\n");
        puml.append("|=Color|=Role|\n");
        puml.append("|<#DCEEFF>|Screens / Activities / Fragments|\n");
        puml.append("|<#E2F5E6>|Models|\n");
        puml.append("|<#FFF3BF>|Adapters|\n");
        puml.append("|<#EFEFEF>|Utils / Firebase|\n");
        puml.append("|<#FFFFFF>|Inner or unconnected support|\n");
        puml.append("endlegend\n");
        puml.append("@enduml\n");

        Files.writeString(PLANTUML_DIR.resolve(fileBaseName + ".puml"), puml.toString(), StandardCharsets.UTF_8);
    }

    private void writeGroupPackage(StringBuilder puml,
                                   String label,
                                   String color,
                                   Set<String> included,
                                   Set<String> unconnected,
                                   boolean showFields,
                                   Group group) {
        List<ClassInfo> groupClasses = classesByQualifiedName.values().stream()
                .filter(info -> included.contains(info.qualifiedName))
                .filter(info -> !unconnected.contains(info.qualifiedName))
                .filter(info -> info.group == group)
                .sorted(Comparator.comparing(info -> info.simpleName))
                .collect(Collectors.toList());

        if (groupClasses.isEmpty()) return;

        puml.append("package \"").append(label).append("\" ").append(color).append(" {\n");
        for (ClassInfo info : groupClasses) {
            writeClass(puml, info, showFields);
        }
        puml.append("}\n\n");
    }

    private void writeClass(StringBuilder puml, ClassInfo info, boolean showFields) {
        String alias = alias(info.qualifiedName);
        String stereotype = stereotype(info.group);
        String label = info.parentQualifiedName == null ? info.simpleName : parentSimpleName(info) + "." + info.simpleName;

        if (showFields && !info.fields.isEmpty()) {
            puml.append(info.kind).append(" \"").append(label).append("\" as ").append(alias)
                    .append(" ").append(stereotype).append(" {\n");
            for (String field : info.fields) {
                puml.append("  ").append(field.replace("\"", "\\\"")).append("\n");
            }
            puml.append("}\n");
        } else {
            puml.append(info.kind).append(" \"").append(label).append("\" as ").append(alias)
                    .append(" ").append(stereotype).append("\n");
        }
    }

    private Set<String> findUnconnected(Set<String> included, Set<Relation> diagramRelations) {
        Set<String> connected = new HashSet<>();
        for (Relation relation : diagramRelations) {
            connected.add(relation.from);
            connected.add(relation.to);
        }
        return included.stream()
                .filter(qname -> !connected.contains(qname))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String formatRelation(Relation relation) {
        String from = alias(relation.from);
        String to = alias(relation.to);
        return switch (relation.type) {
            case EXTENDS -> from + " <|-- " + to;
            case IMPLEMENTS -> from + " <|.. " + to;
            case CONTAINS -> from + " +-- " + to + " : contains";
            case USES -> from + " ..> " + to + " : uses";
        };
    }

    private List<String> sorted(Collection<String> qnames) {
        return qnames.stream()
                .sorted(Comparator.<String, String>comparing(qname -> classesByQualifiedName.get(qname).simpleName)
                        .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    private String alias(String qualifiedName) {
        return qualifiedName.replace(".", "_").replace("$", "_");
    }

    private String stereotype(Group group) {
        return switch (group) {
            case SCREEN -> "<<screen>>";
            case MODEL -> "<<model>>";
            case ADAPTER -> "<<adapter>>";
            case UTIL -> "<<util>>";
            case INNER -> "<<inner>>";
        };
    }

    private String parentSimpleName(ClassInfo info) {
        if (info.parentQualifiedName == null) return "";
        int dot = info.parentQualifiedName.lastIndexOf('.');
        return dot >= 0 ? info.parentQualifiedName.substring(dot + 1) : info.parentQualifiedName;
    }

    private String simplifyType(String type) {
        return type.replace(ROOT_PACKAGE + ".", "")
                .replace("java.lang.", "")
                .replace("java.util.", "");
    }

    private void writeProjectBookSection() throws IOException {
        String hebrewText = "תרשים המחלקות נוצר מתוך קוד הפרויקט בעזרת תוסף UML מתאים. "
                + "התרשים מציג את המחלקות המרכזיות בפרויקט ואת הקשרים ביניהן. "
                + "במקומות שבהם התרשים היה עמוס מדי, חולק התרשים למספר תרשימים נפרדים "
                + "כדי לשמור על קריאות ברורה גם בגרסת ה־PDF.";

        StringBuilder md = new StringBuilder();
        md.append("# Class Diagram Section - Family Tasks\n\n");
        md.append("Generated from real Java source using JavaParser and PlantUML.\n\n");
        md.append("## Full Overview\n\n");
        md.append("![Full overview](images/family_tasks_full_overview.svg)\n\n");
        md.append(hebrewText).append("\n\n");
        md.append("## Screen Classes\n\n");
        md.append("![Screen classes](images/family_tasks_screens.svg)\n\n");
        md.append(hebrewText).append("\n\n");
        md.append("## Model Classes\n\n");
        md.append("![Model classes](images/family_tasks_models.svg)\n\n");
        md.append(hebrewText).append("\n\n");
        md.append("## Adapter Classes\n\n");
        md.append("![Adapter classes](images/family_tasks_adapters.svg)\n\n");
        md.append(hebrewText).append("\n\n");
        md.append("## Utils / Firebase Classes\n\n");
        md.append("![Utils and Firebase classes](images/family_tasks_utils_firebase.svg)\n\n");
        md.append(hebrewText).append("\n");
        Files.writeString(OUTPUT_ROOT.resolve("CLASS_DIAGRAM_SECTION.md"), md.toString(), StandardCharsets.UTF_8);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<html lang=\"he\" dir=\"rtl\">\n<head>\n<meta charset=\"utf-8\">\n");
        html.append("<title>Family Tasks UML Class Diagrams</title>\n");
        html.append("<style>");
        html.append("@page{size:A4 landscape;margin:12mm;}body{font-family:Arial,sans-serif;color:#222;}");
        html.append(".page{break-after:page;}h1{font-size:22px;margin:0 0 10px;}img{max-width:100%;max-height:142mm;display:block;margin:0 auto;}");
        html.append("p{font-size:16px;line-height:1.45;margin:10px 0 0;}");
        html.append("</style>\n</head>\n<body>\n");
        List<String> images = List.of(
                "family_tasks_full_overview.svg",
                "family_tasks_screens.svg",
                "family_tasks_models.svg",
                "family_tasks_adapters.svg",
                "family_tasks_utils_firebase.svg");
        List<String> titles = List.of(
                "תרשים מחלקות מלא",
                "מחלקות מסכים",
                "מחלקות מודלים",
                "מחלקות מתאמים",
                "מחלקות עזר ו-Firebase");
        for (int i = 0; i < images.size(); i++) {
            html.append("<section class=\"page\">\n<h1>").append(titles.get(i)).append("</h1>\n");
            html.append("<img src=\"images/").append(images.get(i)).append("\" alt=\"").append(titles.get(i)).append("\">\n");
            html.append("<p>").append(hebrewText).append("</p>\n</section>\n");
        }
        html.append("</body>\n</html>\n");
        Files.writeString(OUTPUT_ROOT.resolve("class_diagram_pdf_check.html"), html.toString(), StandardCharsets.UTF_8);
    }

    private void writeGenerationReport(List<Path> javaFiles) throws IOException {
        Set<String> allClasses = includeAllProjectClasses();
        Set<String> unconnected = findUnconnected(allClasses, relations);

        StringBuilder report = new StringBuilder();
        report.append("# UML Generation Report\n\n");
        report.append("- Source root: `app/src/main/java`\n");
        report.append("- Java parser: JavaParser from Gradle distribution\n");
        report.append("- UML renderer: PlantUML standalone jar\n");
        report.append("- Java files scanned: ").append(javaFiles.size()).append("\n");
        report.append("- Project classes discovered: ").append(classesByQualifiedName.size()).append("\n");
        report.append("- Project relationships discovered: ").append(relations.size()).append("\n\n");
        report.append("## Discovered Classes\n\n");
        for (ClassInfo info : classesByQualifiedName.values()) {
            report.append("- `").append(info.qualifiedName).append("` (").append(info.group).append(")\n");
        }
        report.append("\n## Unconnected Classes In Full Overview\n\n");
        if (unconnected.isEmpty()) {
            report.append("- None\n");
        } else {
            for (String qname : sorted(unconnected)) {
                report.append("- `").append(qname).append("` placed in the `Unconnected in this diagram` area.\n");
            }
        }
        Files.writeString(OUTPUT_ROOT.resolve("UML_GENERATION_REPORT.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private enum Group {
        SCREEN,
        MODEL,
        ADAPTER,
        UTIL,
        INNER
    }

    private enum RelationType {
        EXTENDS,
        IMPLEMENTS,
        CONTAINS,
        USES
    }

    private static class ParsedUnit {
        final Path path;
        final CompilationUnit unit;

        ParsedUnit(Path path, CompilationUnit unit) {
            this.path = path;
            this.unit = unit;
        }
    }

    private static class ClassInfo {
        String simpleName;
        String qualifiedName;
        String packageName;
        String parentQualifiedName;
        String kind;
        Group group;
        Path sourceFile;
        TypeDeclaration<?> node;
        Map<String, String> imports = new HashMap<>();
        List<String> fields = new ArrayList<>();
        List<String> extendsTypes = new ArrayList<>();
        List<String> implementsTypes = new ArrayList<>();
    }

    private static class Relation {
        final String from;
        final String to;
        final RelationType type;

        Relation(String from, String to, RelationType type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }

        boolean sameEndpoints(Relation other) {
            return from.equals(other.from) && to.equals(other.to);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Relation relation)) return false;
            return from.equals(relation.from) && to.equals(relation.to) && type == relation.type;
        }

        @Override
        public int hashCode() {
            return from.hashCode() * 31 * 31 + to.hashCode() * 31 + type.hashCode();
        }
    }
}
