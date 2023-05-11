# GitHub Workflows description

JSON-LD publication includes the following repos:

1. [spec-jsonld](https://github.com/uncefact/spec-jsonld) - contains transformation code
2. [spec-JSONschema](https://github.com/uncefact/spec-JSONschema/) - as a git module for [spec-jsonld](https://github.com/uncefact/spec-jsonld)
3. [vocabulary-outputs](https://github.com/uncefact/vocabulary-outputs/) - generates HTML presentation of JSON-LD vocabularies
  
Proposed workflow for BSP vocabulary maintance:

## [spec-JSONschema](https://github.com/uncefact/spec-JSONschema/) and [spec-jsonld](https://github.com/uncefact/spec-jsonld)
1. A new version of JSON Schema is published on [spec-JSONschema](https://github.com/uncefact/spec-JSONschema/).
1. A maintainer of [spec-jsonld](https://github.com/uncefact/spec-jsonld) is notified about the new BSP version.
1. A maintainer of [spec-jsonld](https://github.com/uncefact/spec-jsonld) updates [spec-JSONschema](https://github.com/uncefact/spec-JSONschema/) git module reference and raises a PR.
## [spec-jsonld](https://github.com/uncefact/spec-jsonld)
1. A GitHub workflow is triggered on the PR and a new version of transformation code is buit, 
1. The GitHub workflow runs transformation code runs using the new version of JSON Schema, JSON-LD files are produced and added to the PR for review.
1. Tests are executed to confirm the JSON-LD files are valid from structure(json schema) and data(relationships between resources) perspectives.
1. The fixes and adjustments are applied if needed until JSON-LD files are correct and diff report is produced.
1. The PR is approved, merged and a new release is created with JSON-LD vocaularies as release artifacts.
## [spec-jsonld](https://github.com/uncefact/spec-jsonld) and [vocabulary-outputs](https://github.com/uncefact/vocabulary-outputs/)
1. A repository dispatch request is sent to [vocabulary-outputs](https://github.com/uncefact/vocabulary-outputs/) and triggers [preview.yml](https://github.com/uncefact/vocabulary-outputs/blob/main/.github/workflows/preview.yml) workflow.
1. The new JSON-LD vocabularies are downloaded as latest artifacts from [spec-jsonld](https://github.com/uncefact/spec-jsonld).
1. The md and data files are generated from the new version JSON-LD vocabulary.
1. A PR is raised and deployed to a test endpoint for review to https://test.uncefact.org/
1. A broken link check is ran against the test endpoint.
1. The PR is approved and merged and the [release.yml](https://github.com/uncefact/vocabulary-outputs/blob/main/.github/workflows/release.yml) is manually triggered to update https://vocabulary.uncefact.org/
1. A broken link check is ran against the production endpoint.

## Release artifacts:
1. [spec-jsonld](https://github.com/uncefact/spec-jsonld) - the Java application with transformation code, JSON-LD vocabulary and context files, diff report
2. [vocabulary-outputs](https://github.com/uncefact/vocabulary-outputs/) - broken link check report, diff report for vocabularies versions
