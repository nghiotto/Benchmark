/**
 * OWASP Benchmark Project
 *
 * This file is part of the Open Web Application Security Project (OWASP)
 * Benchmark Project For details, please see
 * <a href="https://www.owasp.org/index.php/Benchmark">https://www.owasp.org/index.php/Benchmark</a>.
 *
 * The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details
 *
 * @author Dave Wichers <a href="https://www.aspectsecurity.com">Aspect Security</a>
 * @author Parthi Shah <a href="http://www.iappsecure.com">iAppSecure</a>
 * @created 2016
 *
 * This file reuses existing OWASP Benchmark Project code with Fusion Lite Insight specific changes by Parthi Shah
 */

package org.owasp.benchmark.score.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class FusionLiteInsightReader extends Reader {

    public TestResults parse(File f) throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        // Prevent XXE
        docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        InputSource is = new InputSource( new FileInputStream(f) );
        Document doc = docBuilder.parse(is);

        // Fusion Lite Insight specific changes by Parthi Shah
        TestResults tr = new TestResults("Fusion Lite Insight", true, TestResults.ToolType.Hybrid);

        Node root = doc.getDocumentElement();
        String version = getAttributeValue("version", root);
        tr.setToolVersion(version);

        List<Node> projectList = getNamedChildren("Project", root);
        List<Node> targetsList = getNamedChildren("Targets", projectList);
        List<Node> targetList = getNamedChildren("Target", targetsList);

        for (Node target : targetList) {
            try {
                String targetID = getNamedChild("ID", target).getTextContent();
                String targetURL = getNamedChild("URL", target).getTextContent();

                List<Node> findingsList = getNamedChildren("Findings", target);
                List<Node> findingList = getNamedChildren("Finding", findingsList);

                for (Node finding : findingList) {
                    String findingName = getNamedChild("Name", finding).getTextContent();
                    int findingCWE = Integer.parseInt(getNamedChild("CWE", finding).getTextContent());

                    if (findingCWE != 0) {
                        int testNumber = extractTestNumber(targetURL);
                        if (testNumber != -1) {
                            TestCaseResult tcr = new TestCaseResult();
                            tcr.setCategory(findingName);
                            tcr.setCWE(findingCWE);
                            tcr.setEvidence(findingName);
                            tcr.setNumber(testNumber);
                            tr.put(tcr);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tr;
    }

    private static int extractTestNumber(String uri) throws URISyntaxException {
        // Remove the query and fragment from the URI because some of alert URIs (e.g. generated by DOM XSS) might be malformed
        // (characters that should be escaped are not) which leads to exceptions when parsed by java.net.URI.
        URI url = new URI(removeQueryAndFragment(uri));

        String testfile = url.getPath();
        testfile = testfile.substring( testfile.lastIndexOf('/') +1 );

        if ( testfile.startsWith( "BenchmarkTest" ) ) {
            String testno = testfile.substring("BenchmarkTest".length());
            if ( testno.endsWith( ".html" ) ) {
                testno = testno.substring(0, testno.length() -5 );
            }
            try {
                return Integer.parseInt( testno );
            } catch( NumberFormatException e ) {
                System.out.println( "> Parse error " + testfile + ":: " + testno );
            }
        }
        return -1;
    }

    private static String removeQueryAndFragment(String uri) {
        String strippedUri = uri;
        int idx = strippedUri.indexOf('?');
        if (idx != -1) {
            strippedUri = strippedUri.substring(0, idx);
        }
        idx = strippedUri.indexOf('#');
        if (idx != -1) {
            strippedUri = strippedUri.substring(0, idx);
        }
        return strippedUri;
    }
}
