@Library('MobilePipelineSharedLibrary@master')
import aws.dvsa.mobile.Globals
import aws.dvsa.mobile.CommonFunctions
import aws.dvsa.mobile.GitFunctions
import aws.dvsa.mobile.MobileCoreFunctions

//---------------------------------------------------------
def Globals             = new Globals()
def CommonFunctions     = new CommonFunctions()
def MobileCoreFunctions = new MobileCoreFunctions()
def GitFunctions        = new GitFunctions()
//---------------------------------------------------------
branch          = "${GIT_BRANCH}"
target_location = "${TARGET_LOCATION}"
git_repo_url    = "${GIT_REPO_URL}"
plugin_check    = "${PLUGIN}"

if (plugin_check.toString() == 'Yes') {
    plugin_repo     = "${PLUGIN_REPO}"
    plugin_target   = "${PLUGIN_TARGET}"
    plugin_branch   = "${PLUGIN_BRANCH}"
}

bucket  = "${BUCKET}"
file    = "${IPA_FILE_NAME}"
//---------------------------------------------------------

timestamps {
    ansiColor('xterm') {

        /**
         * The following code will be run on the master node
         */
        node('master') {
            deleteDir()

            /**
             * Checkout code from Git
             */
            stage('Git Checkout') {
                CommonFunctions.log('info', 'STAGE: CHECKOUT')
                parallel(
                        main: {
                            GitFunctions.git_check_out(git_repo_url, branch, target_location, Globals.GITLAB_CREDS)
                        },
                        plugin: {
                            if (plugin_check == 'Yes') {
                                GitFunctions.git_check_out(plugin_repo, plugin_branch, plugin_target, Globals.GITLAB_CREDS)
                            }
                        }
                )

                if (plugin_check == 'Yes') {
                    CommonFunctions.log('info', 'MERGED PLUGIN REPO INTO THE MAIN REPO')
                    sh """
                       mv ${plugin_target} ${target_location}/${plugin_target}
                       """
                }

                CommonFunctions.log('info', 'CODE FETCHED')
            }

            def WORKSPACE = pwd()

            dir("${WORKSPACE}") {
                CommonFunctions.log('info', 'STASHING CODE')
                stash allowEmpty: true, useDefaultExcludes: false, name: "${target_location}", includes: "${target_location}/**"
                CommonFunctions.log('info', 'STASHED')
            }
        }

        /**
         * The following code will be run on the mac in the cloud instance
         */
        node('mac') {
            deleteDir()

            def mac_workspace = pwd()

            CommonFunctions.log('info', 'UNSTASHING CODE')
            unstash "${target_location}"
            CommonFunctions.log('info', 'CODE UNSTASHED')

            dir("${mac_workspace}/${target_location}") {
                /**
                 * NPM Install
                 */
                stage('NPM Install') {
                    CommonFunctions.log('info', 'STAGE: NPM INSTALL')
                    MobileCoreFunctions.install()
                    CommonFunctions.log('info', 'STAGE: NPM INSTALL COMPLETED')
                }

                /**
                 * Run the security checks against the repo this is done using the home office security scanner
                 */
                stage('Security Check') {
                    CommonFunctions.log('info', 'STAGE: SECURITY CHECK')
                    MobileCoreFunctions.security()
                    CommonFunctions.log('info', 'SECURITY CHECKS COMPLETED')
                }

                /**
                 * Run SonarQube linting against the code
                 */
                stage('SonarQube') {
                    CommonFunctions.log('info', 'STAGE: SONARQUBE')
                    MobileCoreFunctions.sonar()
                    CommonFunctions.log('info', 'CODE LINTING COMPLETED')
                }

                /**
                 * Run tests against the code
                 */
                stage('Unit Tests') {
                    CommonFunctions.log('info', 'STAGE: UNIT TESTS')
                    MobileCoreFunctions.testing('unit', ' ', ' ', ' ')
                    CommonFunctions.log('info', 'UNIT TESTS COMPLETED')
                }

                /**
                 * Run npm build
                 */
                stage('NPM Build') {
                    CommonFunctions.log('info', 'STAGE: NPM BUILD')
                    MobileCoreFunctions.npmBuild()
                    CommonFunctions.log('info', 'NPM BUILD COMPLETE')
                }

                CommonFunctions.log('info', 'PLUGIN SET TO: ' + plugin_check)
                if (plugin_check == 'Yes') {
                    /**
                     * If plugin is set to yes then this stage will run. It adds Cordova plugins to the package.json file
                     */
                    stage('Configure Plugin') {
                        CommonFunctions.log('info', 'STAGE: CONFIGURE PLUGIN')
                        MobileCoreFunctions.configurePlugin(plugin_target)
                        CommonFunctions.log('info', 'PLUGIN CONFIGURED SUCCESSFULLY')
                    }
                }

                /**
                 * Configure cordova platform
                 */
                stage('Cordova Platform') {
                    CommonFunctions.log('info', 'STAGE: CORDOVA PLATFORM')
                    MobileCoreFunctions.configure()
                    CommonFunctions.log('info', 'PLATFORM CONFIGURED SUCCESSFULLY')
                }
            }

            dir("${mac_workspace}/${target_location}/platforms/ios") {
                /**
                 * This stage cleans the code, archives the code and exports the archive to a .ipa file
                 */
                stage('Build') {
                    CommonFunctions.log('info', 'STAGE: BUILD')
                    MobileCoreFunctions.build()
                    CommonFunctions.log('info', 'BUILD COMPLETED SUCCESSFULLY')
                }

                stash name: "build", includes: "build/**"
            }
        }

        /**
         * The following code will run on the master node
         */
        node('master') {
            deleteDir()

            unstash "build"

            /**
             * Upload IPA file to S3
             */
            stage('Upload to S3') {
                CommonFunctions.log('info', 'STAGE: UPLOAD TO S3')
                MobileCoreFunctions.upload(bucket, file)
                CommonFunctions.log('info', 'UPLOAD TO S3 COMPLETED')
            }

            /**
             * Generate bucket URL
             */
            stage('Generate bucket URL') {
                CommonFunctions.log('info', 'STAGE: Generate bucket URL')
                MobileCoreFunctions.publish(bucket, file)
                CommonFunctions.log('info', 'BUCKET LINK CREATED')
            }
        }
    }
}