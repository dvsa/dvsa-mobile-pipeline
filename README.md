# DVSA Mobile Pipeline

The following repository contains the core pipeline code for automating the build of IPA files. For the common library
repository please look [here]()

## Getting Started

To use this repository clone it to your local computer and add it to a Jenkins pipeline job.

### Prerequisites

The pipeline calls a shared library which does a lot of the processing. Without this shared library the project will not 
be able to run correct. 

The pipeline also expects the following dependencies to be installed on the Jenkins box:

* Node
* NPM
* ScanRepo
* Cordova 


### Stages

The pipeline has the following stages:

* Git Checkout
* Security Check --> This section uses the Home Office ScanRepo plugin. For more information please look [here](https://github.com/UKHomeOffice/repo-security-scanner)
* SonarQube
* NPM Install
* Unit Tests
* NPM Build
* Configure Plugin
* Cordova Platform
* Build
* Integration Tests --> This section uses an AWS Device Farm plugin. For more information please look [here](https://github.com/awslabs/aws-device-farm-jenkins-plugin)
* Upload to S3
* Generate bucket URL

The resulting stages will end with an S3 URL which will allow you to download the IPA file stored in S3. This URL will expire
after a period of time. If it expires you will not be able to access the bucket contents and will need to get the IPA file manually.

The build section uses Fastlane files to configure the build. For more information please look at [FASTLANE.md](FASTLANE.md)

## Environment Variables

To run the pipeline you'll need to configure the following environment variables:

Variable | Type | Description | Required (Y / N)
--- | --- | ---  |  ---
GIT_BRANCH | String | This is the branch name of the main git repository | Y
TARGET_LOCATION | String | This is the name of the git repository | Y
GIT_REPO_URL | String | The URL of the main git repository | Y
PLUGIN | Choice Parameter | A 'Yes' 'No' choice. This tells the pipeline to run the optional plugin functionality | N
PLUGIN_REPO | String | The repository URL of the plugin repo | N
PLUGIN_TARGET | String | The name of the plugin git repository | N
BUCKET | String | The S3 bucket that stores the IPA file | Y
DEVICE_FARM | Choice Parameter | A 'Yes' 'No' choice. This tells the pipeline to run the integration tests or not | N
UI_AUTOMATION_ARTIFACT | String | The name of your automation artifact| N
DEVICE_FARM_PROJECT_NAME | String | The name of your device farm project | N
IPA_FILE_NAME | String | The name of the IPA file | Y

## Optional Parameters
### Plugin
If the **'PLUGIN'** environment variable is set to **Yes** pipeline will look a provided repository and run 
```
cordova plugin add ${plugin_name}
```
To add that external plugin to the cordova build.
If you don't want to use this functionality set **'PLUGIN'** to **No**
### Device Farm
The **Integration Tests** section uses AWS Device Farm if you don't want to use this functionality set **'DEVICE_FARM'** to **No**
## Security
The pipeline will be accessing S3 and Device Farm. It is recommended that the Jenkins box is configured with an IAM role
giving the pipeline access to those resources but restricting what features it can access from them.

## Common Issues & Fixes
### Archiving
Fastlane has issues code signing sometimes. If you're lane keeps failing on the archive try running this command

```
security unlock-keychain /Users/${user}/Library/Keychains/login.keychain
```

This will unlock your systems keychain giving fastlane access to it.

### Old XCode Project
A common cordova issue is that it will generate an out of date XCode project. If you get this error install the following
plugin to your fastlane build --> https://github.com/ionic-zone/fastlane-plugin-upgrade_super_old_xcode_project

You can then add this code your lane:

```
upgrade_super_old_xcode_project(
    path: xcodeprojpath,
    team_id: team_id
)
```

The upgrade_super_old_xcode_project plugin will update your XCode project to a version fastlane can understand
## Contributing

To contribute please create a pull request 

## Authors

* **Alex Le Peltier** - *Initial work*

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details

