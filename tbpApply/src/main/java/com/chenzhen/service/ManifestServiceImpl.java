package com.chenzhen.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManifestServiceImpl implements ManifestService{
    private final static String dataDictStr1 = "<dataField name=\"";
    private final static String dataDictStr2 = "\" desc=\"";
    private final static String dataDictStr3 = "\" />";


    private final static String escTempStr1 = "<";
    private final static String escTempStr2 = ">$!";
    private final static String escTempStr3 = "</";
    private final static String escTempStr4 = ">";

    private final static String msgDefineStr1 = "<gw:field name=\"";
    private final static String msgDefineStr2 = "\" refName=\"";
    private final static String msgDefineStr3 = "\" need=\"false\"  desc=\"";
    private final static String msgDefineStr4 = "\" />";

    @Override
    public List<String> getOrderList(String orderO) {
        List<String> list = new ArrayList<String>();
        List<String> outputList = new ArrayList<String>();
        list.add("soa_corporService");
        list.add("soa_TBPAuthenticationService");
        list.add("TBPinnerManagement");
        list.add("TBPinnerService");
        list.add("TBPtaskMan");
        list.add("transBank");
        list.add("YnetIfpCommon");
        list.add("firsmanage");

        String[] msgs = orderO.split("\n");
        for (String msg : msgs) {
            msg = msg.replaceAll("\\\\", "/");
            for (String str : list) {
                String target = "";
                if (msg.contains("/" + str + "/")) {
                    String[] ss = msg.split("/" + str + "/");
                    String ss1 = ss[1];
                    String desc1 = "designSource/";
                    String desc2 = "WebContent/";
                    String desc3 = "src/";
                    String desc4 = "firs/";
                    String desc5 = "etc/";
                    if (ss1.contains(desc1)) {
                        String[] sss = ss1.split(desc1);
                        String ss2 = sss[1];
                        if (ss2.endsWith(".bl")) {
                            int index = ss2.lastIndexOf(".");
                            target = "WEB-INF/conf/" + ss2.substring(0, index) + ".xml";
                        } else {
                            target = "--" + ss2;
                        }
                        outputList.add(target);
                    } else if (ss1.contains(desc2)) {
                        String[] sss = ss1.split(desc2);
                        target = sss[1];
                        outputList.add(target);
                    } else if (ss1.contains(desc3)) {
                        String[] sss = ss1.split(desc3);
                        String ss2 = sss[1];
                        if (ss2.endsWith(".java")) {
                            int index = ss2.lastIndexOf(".");
                            target = "WEB-INF/classes/" + ss2.substring(0, index) + ".class";
                            try {
                                String tmpTarget = target;
                                int index1 = msg.lastIndexOf(".");
                                int index2 = msg.lastIndexOf("/");
                                String className = msg.substring(index2 + 1, index1);
                                String filepath = msg.substring(0, index2);
                                filepath = filepath.replace("/src/", "/WebContent/WEB-INF/classes/");
                                File file = new File(filepath);
                                File[] files = file.listFiles();
                                for (File f : files) {
                                    if (f.isFile()) {
                                        String filename = f.getName();
                                        int index3 = filename.lastIndexOf(".");
                                        filename = filename.substring(0, index3);
                                        if (filename.substring(0, filename.length() - 2).equals(className)) {
                                            int index4 = tmpTarget.lastIndexOf("/");
                                            String target2 = tmpTarget.substring(0, index4 + 1).concat(filename).concat(".class");
                                            target = target + "\n" + target2;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                        } else {
                            target = "--" + ss2;
                        }
                        outputList.add(target);
                    } else if (ss1.startsWith(desc4)) {
                        String ss2 = ss1.replaceFirst(desc4, "");
                        if (ss2.endsWith(".java")) {
                            int index = ss2.lastIndexOf(".");
                            target = "WEB-INF/classes/" + ss2.substring(0, index) + ".class";
                            try {
                                String tmpTarget = target;
                                int index1 = msg.lastIndexOf(".");
                                int index2 = msg.lastIndexOf("/");
                                String className = msg.substring(index2 + 1, index1);
                                String filepath = msg.substring(0, index2);
                                filepath = filepath.replace("/src/", "/WebContent/WEB-INF/classes/");
                                File file = new File(filepath);
                                File[] files = file.listFiles();
                                for (File f : files) {
                                    if (f.isFile()) {
                                        String filename = f.getName();
                                        int index3 = filename.lastIndexOf(".");
                                        filename = filename.substring(0, index3);
                                        if (filename.substring(0, filename.length() - 2).equals(className)) {
                                            int index4 = tmpTarget.lastIndexOf("/");
                                            String target2 = tmpTarget.substring(0, index4 + 1).concat(filename).concat(".class");
                                            target = target + "\n" + target2;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                        } else {
                            target = "--" + ss2;
                        }
                        outputList.add(target);

                    } else if (ss1.startsWith(desc5)) {
                        String ss2 = ss1.replaceFirst(desc5, "");
                        ss2 = "WEB-INF/classes/" + ss2;
                        outputList.add(ss2);
                    } else {
                        outputList.add(ss1);
                    }
                }
            }
        }

        return outputList;
    }

    @Override
    public List<String> datadict(String orderO) {
        String[] str = orderO.split("\n");
        List<String> list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(dataDictStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(dataDictStr2);
            sb.append(s.substring(s.indexOf(9) + 1, s.length()));
            sb.append(dataDictStr3);
            String r = sb.toString();
            return r;
        }).collect(Collectors.toList());
        return list;
    }

    @Override
    public List<String> requestData(String orderO) {
        String[] str = orderO.split("\n");
        List list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(escTempStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(escTempStr2);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(escTempStr3);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(escTempStr4);
            String r = sb.toString();
            return r;
        }).collect(Collectors.toList());

        return list;
    }

    @Override
    public List<String> responseData(String orderO) {
        String[] str = orderO.split("\n");
        List list = Arrays.asList(str).stream().map(s->{

            StringBuffer sb = new StringBuffer();
            sb.append(msgDefineStr1);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(msgDefineStr2);
            sb.append(s.substring(0, s.indexOf(9)));
            sb.append(msgDefineStr3);
            sb.append(s.substring(s.indexOf(9) + 1, s.length()));
            sb.append(msgDefineStr4);
            String r = sb.toString();
            return r;
        }).collect(Collectors.toList());

        return list;
    }
}
