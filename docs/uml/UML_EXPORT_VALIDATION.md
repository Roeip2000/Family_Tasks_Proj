# UML Export Validation

## Tooling
- Code parser/generator: `docs/uml/tools/GenerateFamilyTasksUml.java`
- Parser library: JavaParser `javaparser-core-3.27.1` from the local Gradle distribution cache
- UML renderer: PlantUML standalone `docs/uml/tools/plantuml.jar`
- Source scanned: `app/src/main/java`

## Generated Diagrams
- Full overview: `docs/uml/images/family_tasks_full_overview.svg` and `.png`
- Screen classes: `docs/uml/images/family_tasks_screens.svg` and `.png`
- Model classes: `docs/uml/images/family_tasks_models.svg` and `.png`
- Adapter classes: `docs/uml/images/family_tasks_adapters.svg` and `.png`
- Utils/Firebase classes: `docs/uml/images/family_tasks_utils_firebase.svg` and `.png`

## PNG Export Sizes
- `family_tasks_full_overview.png`: 8192 x 2536
- `family_tasks_screens.png`: 8192 x 2536
- `family_tasks_models.png`: 2030 x 1549
- `family_tasks_adapters.png`: 5930 x 1906
- `family_tasks_utils_firebase.png`: 5595 x 3694

## PDF Check
- PDF proof file: `docs/uml/pdf/family_tasks_class_diagrams_pdf_check.pdf`
- The PDF proof was generated from the SVG exports on separate A4 landscape pages.
- The PDF contains 5 pages, one page per diagram.
- Direct PlantUML PDF export was not used because this standalone jar did not include Batik's PDF converter classes; SVG-to-PDF browser printing kept the diagrams vector-based for the readability proof.
- The split diagrams are the recommended versions for the project book because they stay readable after PDF insertion.
- The full overview is useful as an appendix or overview page, but it is denser than the split diagrams.
- The original SVG and PNG files are kept separately in `docs/uml/images/` in case a document editor compresses the PDF image too much.

## Unconnected Classes
- `ParentInFb` was detected from source but has no project-to-project relationship in the generated full overview.
- It is placed in the `Unconnected in this diagram` area of the full overview diagram.
