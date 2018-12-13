# Change log
This change log will act as a record of all changes to the mobile pipeline repo.

## 0.0.1 -> 7-12-2018
### Added
- Core pipeline initial build
- CHANGELOG.md for the project
- README.md for the project
- Git ignore file

## 0.0.2 -> 13-12-2018
- Adding logic to not expect plugin variables if plugin is set to **No**
- Moving NPM install before SonarQube
- Moving stages to the Mac node rather than the master node
- Adding code to delete the directory on the mac before running
- Improved logging