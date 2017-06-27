/*
    MIT License

    Copyright (c) 2017 Kilian Stein

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/
package org.bonitasoft.quality

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class BonitaExtractSourcesTest extends Specification {
    public static final String WORKSPACE_NAME_TEST = "ProjectBonita"

    public Path workspace

    def setup() {
        workspace = Paths.get(getResourcePath(WORKSPACE_NAME_TEST))
        new BonitaExtractSources(System.out).deleteDir(workspace.resolve(BonitaExtractSources.EXTRATED_DIR))
    }

    private static String getResourcePath(String relativePath) {
        return BonitaExtractSourcesTest.class.getClassLoader().getResource(relativePath).getPath().replaceFirst("/", "");
    }

    def testExtract() {
        when:
        new BonitaExtractSources(System.out).extract(workspace.toFile().getAbsolutePath())

        then:
        Path dir = workspace.resolve(BonitaExtractSources.EXTRATED_DIR)
        dir.toFile().exists()
    }

    def testExtractDiagramScript() {
        when:
        new BonitaExtractSources(System.out).extractScriptGroovyFromDiagrams(workspace)

        then:
        Path diagramScriptsDir = workspace.resolve(BonitaExtractSources.DIR_EXTRACTED_DIAGRAMS)
        diagramScriptsDir.toFile().exists()
        List<String> diagramsScriptsDir = diagramScriptsDir.toFile().list()
        diagramsScriptsDir.size() == 2
        diagramsScriptsDir.contains("Example-CancelVacationRequest")
        diagramsScriptsDir.contains("Example-NewVacationRequest")
        Path filesScriptPath = diagramScriptsDir.resolve("Example-CancelVacationRequest")
        filesScriptPath.resolve("isRequestStatusApproved.groovy").toFile().exists()
        List<String> filesScript = filesScriptPath.toFile().list()
        filesScript.size() == 7
        List<String> fileScript = Files.readAllLines(filesScriptPath.resolve("isRequestStatusApproved.groovy"))
        fileScript[0] == "return vacationRequestToCancel.status == \"approved\""
    }

    def testExtractWidgetsHtml() {
        when:
        new BonitaExtractSources(System.out).extractHtmlFromWebWidget(workspace)

        then:
        Path widgetsHtmlDir = workspace.resolve(BonitaExtractSources.DIR_EXTRACTED_WEB_WIDGET)
        widgetsHtmlDir.toFile().exists()
        List<String> widgetHtmlDir = widgetsHtmlDir.toFile().list()
        widgetHtmlDir.size() == 2
        widgetHtmlDir.contains("pbDataTable.html")
        widgetHtmlDir.contains("pbChecklist.html")
        List<String> fileScript = Files.readAllLines(widgetsHtmlDir.resolve("pbChecklist.html"))
        fileScript[0] == """<div class="row form-group" ng-class="{ 'form-horizontal':  !properties.labelHidden && properties.labelPosition === 'left' }">"""
    }

    def testExtractJavascriptPage() {
        when:
        new BonitaExtractSources(System.out).extractJavascriptFromWebPage(workspace)

        then:
        Path dir = workspace.resolve(BonitaExtractSources.DIR_EXTRACTED_WEB_PAGE)
        dir.toFile().exists()
        List<String> widgetHtmlDir = dir.toFile().list()
        widgetHtmlDir.size() == 2
        widgetHtmlDir.contains("0096d371-549a-4210-a02c-2f915198a374.js")
        widgetHtmlDir.contains("115e20c0-3948-4130-8d17-5a9a8c10784b.js")
        List<String> fileScript = Files.readAllLines(dir.resolve("115e20c0-3948-4130-8d17-5a9a8c10784b.js"))
        fileScript[3] == """  \$data.copySelectedRow = function (\$data){"""
    }

    def testExtractJavascripFragments() {
        when:
        new BonitaExtractSources(System.out).extractJavascriptFromFragment(workspace)

        then:
        Path dir = workspace.resolve(BonitaExtractSources.DIR_EXTRACTED_WEB_FRAGMENTS)
        dir.toFile().exists()
        List<String> widgetHtmlDir = dir.toFile().list()
        widgetHtmlDir.size() == 1
        widgetHtmlDir.contains("4c75e7bc-40e1-43d6-ac27-5e89a71aa8d8.js")
        List<String> fileScript = Files.readAllLines(dir.resolve("4c75e7bc-40e1-43d6-ac27-5e89a71aa8d8.js"))
        fileScript[3] == """  \$data.formInput = { "cancellationApprovedContract" : false } ;"""
    }

    def testCleanVersion(){
        when:
        String name = "Example-CancelVacationRequest-1.2.0"
        String nameSnapShot = "Example-CancelVacationRequest-1.2.0-SNAPSHOT"
        String nameRC1 = "Example-CancelVacationRequest-1.2.0-RC1"
        String nameRC12 = "Example-CancelVacationRequest-1.2.0-RC12"
        then:
        "Example-CancelVacationRequest" == BonitaExtractSources.cleanVersion(name)
        "Example-CancelVacationRequest" == BonitaExtractSources.cleanVersion(nameSnapShot)
        "Example-CancelVacationRequest" == BonitaExtractSources.cleanVersion(nameRC1)
        "Example-CancelVacationRequest" == BonitaExtractSources.cleanVersion(nameRC12)
    }
}