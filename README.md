#Bonita Extract Sources

## Introduction
Bonitasoft is a software to create a BPM application all-in-one, from back-end to front end, all in the same environment. 
We can drag tasks into diagrams or widgets into web-pages with beautiful rendering.
We can add some code (groovy, javascript, css, html) to be closer to our needs. 

What's about this specific code insides diagrams and web-pages ?
How do I know the level of quality of this code ?
How can I detect potential misconception, code smell or bug ?

This is what _Bonita Extract Sources_ provide you : A way to analyse code hidden in diagrams or web-pages

We will see how we can connect Bonitasoft with SonarQube

## Conception

The project is a script-like compile in an executable jar. 
This script parse bonita workspace and find resources with hidden code.
Then an extraction of code is executed to a new directory called `extracted_sources`

Supported bonita extractions are :
 * groovy script extracted from diagrams
 * html extracted from web widgets
 * javascript extracted from web pages
 * javascript extracted from web fragments

## Compatibility version

This version in tested with Bonitasoft 7.2.x

## Usage
Two way to get executable jar `bonita-extract-sources-X.X.jar` :
1. maven clean install
2. get jar from GitHub release

Two way to use it :
1. `java -jar bonita-extract-sources-X.X.jar /myPath/WorskpspacePath`
2. `cd /myPath/WorskpspacePath' and 'java -jar BONITA_EXTRACT_HOME/bonita-extract-sources-X.X.jar .`

**Be careful : do not to commit extracted files**

## Analyse bonita code with SonarQube

The next step is to analyse code with SonarQube :
 
In your Bonita project, copy the file `sonar-project.properties` and update following variables:
* rest-api projects (my-api-rest-1,my-api-rest-2,my-api-rest-3)
* sonar.projectVersion
* sonar.projectKey
* sonar.projectName

Download and install your **Instance SonarQube** and the tools **SonarQube Scanners** : https://www.sonarqube.org/downloads/

In your SonarQube instance (localhost:9000 by default), you have to install following plugs-in :
* Svn plug-in
* Groovy plug-in
* SonarJs Plug-in
* Web Plug-in

With the scanner, analyse your bonita project (it will read `sonar-project.properties`) and generate report to you sonarQube instance.

Bonitasoft is now connected to SonarQube

## Adding RestAPI test and coverage to SonarQube

The file `exemplePomWithCoverageJacoco.xml` contain a example to add coverage to your api rest extension.
Specific coverage with Jacoco description is delimited by  `<!-- Begin Analysis Jacoco -->` and `<!-- End Analysis Jacoco -->`

Before running SonarQube analysis do a `maven clean install` or  compile with BonitaStudio.

## Continuous integration 

This new bricks works well with a continuous integration. Orchestration is :
1. extract sources
2. mvn clean install for all rest api extension
3. run sonarqube analysis
4. enjoy reports 

Previous orchestration has been tested with jenkins and works well.