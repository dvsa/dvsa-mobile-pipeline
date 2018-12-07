# Fastlane Setup

## Introduction
Fastlane is designed for automating the creation of IPA files. For more information on fastlane please see the docs [here](https://docs.fastlane.tools/)

## Setup
The pipeline requires a fastlane directory to be set up containing a FastFile this file contains the configuration which 
fastlane will run. <br>

You can do a lot with fastlane files but here is an example configuration 

```
platform :ios do
  desc "Description of what the lane does"
  lane :build do
   upgrade_super_old_xcode_project(
    path: "${project}",
    team_id: "${team_id}"
  )
  disable_automatic_code_signing(path: "${project}")
  enable_automatic_code_signing(path: "${project}")
  update_project_team(
   path: "${project}",
   teamid: "${team_id}")
   build_app(
    project: "${project}",
        scheme: "${scheme_name}",
        clean: true,
        output_directory: "./build",
        output_name: "MyTestIPA.ipa",
        export_method: "enterprise",
        codesigning_identity: "iPhone Developer"
   )
  end
end
```

This configuration will archive and export your IPA file into a build folder. The pipeline expects that the pipeline lane
you'd like to run is called build. Any other lanes will not be run unless they're stated in the pipeline library.