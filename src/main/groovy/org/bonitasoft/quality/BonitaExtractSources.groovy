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

import groovy.io.FileType
import groovy.json.JsonSlurper

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Class for sources extraction of XML and json from Bonita. Extractions supported :
 * <ul>
 *    <li>groovy script extracted from diagrams</li>
 *    <li>html extracted from web widgets/li>
 *    <li>javascript extracted from web pages</li>
 *    <li>javascript extracted from web fragments</li>
 * </ul>
 */
class BonitaExtractSources {

    private final PrintStream printStream

    protected static final EXTRATED_DIR = "extracted_sources/"

    private static final DIR_DIAGRAMS = "diagrams"
    protected static final DIR_EXTRACTED_DIAGRAMS = EXTRATED_DIR + DIR_DIAGRAMS

    private static final DIR_WEB_WIDGET = "web_widgets"
    protected static final DIR_EXTRACTED_WEB_WIDGET = EXTRATED_DIR + DIR_WEB_WIDGET


    private static final DIR_WEB_PAGE = "web_page"
    protected static final DIR_EXTRACTED_WEB_PAGE = EXTRATED_DIR + DIR_WEB_PAGE

    private static final DIR_WEB_FRAGMENTS = "web_fragments"
    protected static final DIR_EXTRACTED_WEB_FRAGMENTS = EXTRATED_DIR + DIR_WEB_FRAGMENTS

    BonitaExtractSources(PrintStream printStream) {
        this.printStream = printStream
    }

    static void main(String[] args){
        if (args && args.size() == 1){
            if (Paths.get(args[0]).toFile().exists()){
                new BonitaExtractSources(System.out).extract(args[0])
            } else {
                println("Workspace path is not valide")
            }
        } else {
            println("usages : ")
            println("  'java -jar bonita-extract-sources-X.X.jar /myPath/WorskpspacePath'")
            println("                          OR")
            println("  'cd /myPath/WorskpspacePath' and 'java -jar BONITA_EXTRACT_HOME/bonita-extract-sources-X.X.jar .'")
        }
    }

    /** Main method to be call in jenkins */
    void extract(String pathWorkspaceBonita) {
        Path workspace = Paths.get(pathWorkspaceBonita)
        log("Working directory : " + toStringPath(workspace))
        deleteDir(workspace.resolve(EXTRATED_DIR))
        extractScriptGroovyFromDiagrams(workspace)
        extractHtmlFromWebWidget(workspace)
        extractJavascriptFromWebPage(workspace)
        extractJavascriptFromFragment(workspace)
    }

    /**
     * Parse all diagram xml -> find all groovy -> store them into file
     */
    protected void extractScriptGroovyFromDiagrams(Path workspace) {
        Path dirFilesDiagrams = workspace.resolve(DIR_DIAGRAMS)
        Path dirFilesGroovy = workspace.resolve(DIR_EXTRACTED_DIAGRAMS)

        List<File> filesDiagram = []
        dirFilesDiagrams.toFile().eachFileRecurse(FileType.FILES) { file ->
            if (file.getName().endsWith(".proc")) {
                filesDiagram << file
            }
        }

        for (File fileDiagram : filesDiagram) {
            Map<String, String> contents = extractScriptGroovyFromDiagram(fileDiagram)
            String nameProcessus = cleanVersion(fileDiagram.getName())
            Path dirScriptGroovy = dirFilesGroovy.resolve(nameProcessus)
            saveContents("scripts from diagram process '$nameProcessus'", contents, dirScriptGroovy, ".groovy")
        }
    }

    /**
     * Save contents to a file with a specific extension.
     * @param contents : key is the name and value is the content
     */
    private saveContents(String message, Map<String, String> contents, Path ouputPath, String extension) {
        printStream.println "\nExtraction of $message in directory : '" + toStringPath(ouputPath) + "'"
        ouputPath.toFile().mkdirs()
        contents.each { name, content ->
            log("    - " + name + extension)
            File file = ouputPath.resolve(name + extension).toFile()
            file << content
        }
    }

    /** Delete version from diagram name */
    protected static String cleanVersion(String diagramFullname) {
        String diagramFullnameCleanUnreleased = diagramFullname.replace("-SNAPSHOT", "").replaceAll("-RC[0-9]*", "")
        return diagramFullnameCleanUnreleased.substring(0, diagramFullnameCleanUnreleased.lastIndexOf("-"))
    }

    /** extract groovy scripts from digram */
    private static Map<String, String> extractScriptGroovyFromDiagram(File fileDiagram) {
        Node pathResult = new XmlParser().parse(fileDiagram)
        Map<String, String> contents = [:]
        pathResult.depthFirst().findAll() {
            Node it ->
                def attributes = it.attributes()
                if (attributes.content && attributes.interpreter == "GROOVY" && attributes.type == "TYPE_READ_ONLY_SCRIPT") {
                    String name = getName(contents, String.valueOf(attributes.name))
                    contents.put(name, String.valueOf(attributes.content))
                }
        }
        return contents
    }

    /**
     * Get the name of a content and increment if name already exist.
     * Change illegal caracter for a file
     */
    private static String getName(Map contents, String name) {
        String nameNormalized = name.replaceAll("[:<>\"/|?*]", "_").replace("\\\"", "_")
        if (contents.get(nameNormalized)) {
            return getName(contents, nameNormalized, (int) 1)
        }
        return nameNormalized
    }

    private static String getName(Map contents, String name, int increment) {
        if (contents.get(name + "_" + increment)) {
            return getName(contents, name, increment + 1)
        }
        return name + "_" + increment
    }

    /** Extract html from web widget */
    protected void extractHtmlFromWebWidget(Path workspace) {
        Path dirWebWiget = workspace.resolve(DIR_WEB_WIDGET)
        Path dirOutput = workspace.resolve(DIR_EXTRACTED_WEB_WIDGET)

        Map<String, String> contents = [:]
        dirWebWiget.traverse(type: FileType.FILES, nameFilter: ~/.*\.json/) {
            Path it ->
                String name = getName(contents, it.toFile().getName().replace(".json", ""))
                contents.put(name, (String) new JsonSlurper().parse(it.toFile()).template)
        }
        saveContents("sources from web widgets", contents, dirOutput, ".html")
    }

    /** Extract javascript from web page */
    protected void extractJavascriptFromWebPage(Path workspace) {
        Path dirWebPage = workspace.resolve(DIR_WEB_PAGE)
        Path dirOutput = workspace.resolve(DIR_EXTRACTED_WEB_PAGE)


        Map<String, String> contents = [:]
        dirWebPage.traverse(type: FileType.FILES, nameFilter: ~/.*\.json/) {
            Path it ->
                if (it.parent.parent.endsWith(DIR_WEB_PAGE)) {
                    String name = getName(contents, it.toFile().getName().replace(".json", ""))
                    contents.put(name, generateJavascriptFromUIDesignerJson(new JsonSlurper().parse(it.toFile())))
                }
        }
        saveContents("sources from web pages", contents, dirOutput, ".js")
    }

    /** Extract javascript from fragment */
    protected void extractJavascriptFromFragment(Path workspace) {
        Path dir = workspace.resolve(DIR_WEB_FRAGMENTS)
        Path dirOutput = workspace.resolve(DIR_EXTRACTED_WEB_FRAGMENTS)

        Map<String, String> contents = [:]
        dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.json/) {
            Path it ->
                if (it.parent.parent.endsWith(DIR_WEB_FRAGMENTS)) {
                    String name = getName(contents, it.toFile().getName().replace(".json", ""))
                    contents.put(name, generateJavascriptFromUIDesignerJson(new JsonSlurper().parse(it.toFile())))
                }
        }
        saveContents("sources from fragments", contents, dirOutput, ".js")
    }

    /** Generate javascript from json bonita */
    private static String generateJavascriptFromUIDesignerJson(def json) {
        List data = extractDataFromUIDesignerJson(json)

        String content = ""
        data.each {
            def variable ->
                String name = variable.name, exposed = variable.exposed, type = variable.type, value = variable.value
                if (type == "json") {
                    content += generateJavascriptFromTypeJson(name, exposed, type, value)
                } else if (type == "expression") {
                    content += generateJavascriptFromTypeExpression(name, exposed, type, value)
                }
        }
        return  "function myFunction(" + getDataParameter(content) + ") {" + content + "\n}"
    }

    /** Generate javascript from expression data*/
    private static String generateJavascriptFromTypeExpression(String name, String exposed, String type, String value) {
        String valueWithTabulation = addTabulations(value)
        return getEntete(name, exposed, type) + "  \$data.$name = function (" + getDataParameter(valueWithTabulation) + "){$valueWithTabulation\n  };"
    }

    /** $data parameter is only necessary only if it used */
    private static String getDataParameter(String content){
        return content.contains("\$data") ? "\$data" : ""
    }

    private static String addTabulations(String chaine) {
        String chaineWithTab = ""
        chaine.split("\n").each {
            it ->
                chaineWithTab += "\n" + "    " + it
        }
        return chaineWithTab
    }

    /** Generate javascript from expression json */
    private static String generateJavascriptFromTypeJson(String name, String exposed, String type, String value) {
        return getEntete(name, exposed, type) + "  \$data.$name = $value;"
    }

    private static String getEntete(String name, String exposed, String type) {
        return "\n\n  // name=$name, exposed=$exposed, type=$type\n"
    }

    private static List extractDataFromUIDesignerJson(def json) {
        List data = []
        json.data.each {
            Map.Entry<String, Map> variable ->
                data << extractDataFromUIDesignerJson(variable)
        }
        return data
    }

    private static Map extractDataFromUIDesignerJson(Map.Entry<String, Map> variableJson) {
        Map variable = [:]
        variable.name = variableJson.getKey()
        variableJson.getValue().each {
            Map.Entry configuration ->
                String key = configuration.getKey()
                variable."$key" = configuration.getValue()
        }
        return variable
    }

    protected void deleteDir(Path dirToDelete) {
        File dirFileToDelete = dirToDelete.toFile()
        if (dirFileToDelete.exists()) {
            log("Suppression du repertoire " + dirFileToDelete.getName())
            dirFileToDelete.deleteDir()
        }
    }


    private static String toStringPath(Path path) {
        path.toFile().getAbsolutePath()
    }

    private void log(String message) {
        if (printStream) {
            printStream.println(message)
        }
    }
}
