Release Procedure for ncWMS
===========================

Prerequisites
-------------

### GPG
You should have gpg and gpg-agent installed.  You will need to have imported the ReSC key, which can be found on the ReSC drive at ReSC_software/resc_private_gpg.secret.

This can be imported using the command:
```
gpg --allow-secret-key-import --import <ReSC_drive>/ReSC_software/resc_private_gpg.secret
```

The key is passphrase-protected with the standard ReSC password.

### Maven settings
Maven needs to be installed to build the software, but to release it needs some specific settings added to ~/.m2/settings.xml.  This should contain the following:
```xml
<settings>
  <servers>
    <server>
      <id>edal-snapshots</id>
      <username>resc</username>
      <password>the_resc_password</password>
    </server>
    <server>
      <id>edal-java-release</id>
      <username>resc</username>
      <password>the_resc_password</password>
    </server>
  </servers>
</settings>
```

Release Procedure
-----------------

Once all code is ready to be released, all tests pass, **and all documentation is updated**, the following steps should be taken:

### Create a branch to do the release on:
```
git checkout -b release-VERSION
```

### Set the release versions:
Edit the `pom.xml` file and change:
* The version of the pom to the correct version (normally this is just removing the -SNAPSHOT suffix)
* The version of EDAL to use in the properties.  This should match the version of ncWMS but be one major version lower - e.g. ncWMS 2.1 uses EDAL 1.1

Edit the `Dockerfile` and change:
* The EDAL_VERSION variable to point to the git tag of EDAL to use (`edal-x.y.z`)

### Build the software:
```
mvn clean install
```

### Commit and tag the release:
```
git commit -a -m "Update pom files for release VERSION"
git tag ncwms-VERSION
```

### Deploy to sonatype:
```
mvn deploy -P release
```
Upon successful completion of this stage, log into [sonatype](http://oss.sonatype.org) with the username "resc", click the "Staging Repositories" link on the left, and scroll down to find the uk.ac.rdg.resc entry.  Select it and then click the "Release" button and enter a short comment.  This will allow the releases to be synchronised to Maven central.

### Merge the release branch into master:
```
git checkout master
git merge release-VERSION
git push origin master
git push --tags
```

### Create a release on github:
Go to [the project page on github](https://github.com/Reading-eScience-Centre/ncwms) and click the "Releases" link.  Go to the ncwms-VERSION release and click the "Edit tag" button.  You should now fill in the appropriate boxes and upload ncWMS.war, ncWMS-standalone.jar, and licence.txt as binary attachments.

### Upload the site documents
Pushing the release to master will trigger a build of the ncWMS User Guide onto gitbooks.com.

The github website is stored in the gh-pages branch, and will always point to the latest release and the current version of the ncWMS User Guide.  The only thing which should need updating is the API documents, which you do by:  

```
mvn javadoc:aggregate
git add apidocs
git stash save
git checkout gh-pages
git rm -r apidocs
git stash pop
git commit -m "Updated apidocs"
git push origin gh-pages
```

Prepare for next development iteration
--------------------------------------
### Move to the develop branch and update:
```
git checkout develop
git merge master
```

### Set the snapshot versions:
You should update the versions to the next snapshot version.  Usually this will be an update to the minor number.  For example if the last release was 2.2, the next version would be 2.3-SNAPSHOT.  You should also modify the version of EDAL to be used

### Commit:
```
git commit -a -m "Prepare for next development version"
git push origin develop
```
