package com.github.tacomonkey11;

import com.github.tacomonkey11.model.Course;
import com.github.tacomonkey11.model.Evaluation;
import com.github.tacomonkey11.model.Subject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class TeachAssistParser {

    public static List<Course> parseCourses(String html) {
        ArrayList<Course> courses = new ArrayList<>();

        Document mainPage = Jsoup.parse(html);

        mainPage.select("div.green_border_message:nth-child(2) > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1)").first().children().remove(0);

        Elements courseTable = mainPage.select("div.green_border_message:nth-child(2) > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1)").first().children();

        for (Element element : courseTable) {
            String courseCode = element.select("td:nth-child(1)").toString().split("<br>")[0].replace("<td>", "").replaceFirst(":.+$", "").replaceFirst("-.+$", "").trim();
            String courseName = element.select("td:nth-child(1)").toString().split("<br>")[0].replace("<td>", "").replaceFirst("^.+?(?=:)", "").replace(": ", "").trim();
            String period = element.select("td:nth-child(1)").toString().split("<br>")[1].replace("</td>", "").replace("Block: ", "").split("-")[0].trim();
            String roomNumber = element.select("td:nth-child(1)").toString().split("<br>")[1].replace("</td>", "").replace("Block: ", "").split("-")[1].replace("rm.", "").trim();

            boolean isOpen = !element.select("td:nth-child(3)").toString().contains("Please see teacher for current status regarding achievement in the course");

            if (isOpen) {
                String subjectId = element.select("td:nth-child(3)").select("a").attr("href").replaceFirst("^.+?(?==)", "").replace("=1", "").replaceFirst("&.+$", "").replace("=", "");

                courses.add(new Course(courseCode, period, courseName, roomNumber, true, subjectId, element.select("td:nth-child(3)").select("a").attr("href").contains("viewReportOE")));
            } else {
                courses.add(new Course(courseCode, period, courseName, roomNumber, false, null, false));
            }

        }

        return courses;
    }

    public static Subject parseSubject(String html, boolean v4) {
        Document mainPage = Jsoup.parse(html);

        if (v4) {
            Elements yellowBox = mainPage.select(".yellow");
            double termWeight = Double.parseDouble(yellowBox.select("h2:nth-child(1)").text().replace("Term Work {", "").replaceFirst("}.+$", ""));
            double culmWeight = Double.parseDouble(yellowBox.select("h2:nth-child(2)").text().replace("Culminating Work {", "").replaceFirst("}.+$", ""));

            double totalTermWeight = 0;
            double totalCulmWeight = 0;

            Elements productTable = mainPage.select(".row > table:nth-child(13) > tbody:nth-child(1)").first().children();

            productTable.remove(0);

            List<Evaluation> evaluations = new ArrayList<>();

            for (Element element : productTable) {
                String evalName = element.select("td:nth-child(1)").text();

                double evalMark = 0.0;
                double weight;

                try {
                    evalMark = Double.parseDouble(element.select("td:nth-child(3)").text()) / Double.parseDouble(element.select("td:nth-child(4)").text().replace("/", "").trim()) * 100;
                    weight = Double.parseDouble(element.select("td:nth-child(5)").text());
                } catch (NumberFormatException e) {
                    Elements markTable = mainPage.select(".row > table:nth-child(4) > tbody:nth-child(1)").first().children();
                    markTable.remove(0);
                    markTable.removeIf(section -> section.childrenSize() < 3);

                    for (Element mark : markTable) {
                        if (mark.select("td:nth-child(1)").text().contains(element.select("td:nth-child(2)").text()))
                            evalMark = Double.parseDouble(mark.select("td:nth-child(3)").text().split("%")[0]);
                    }

                    weight = Double.parseDouble(element.select("td:nth-child(4)").text());
                }

                totalTermWeight += weight;
                evaluations.add(new Evaluation(evalName, List.of(new Evaluation.Category("Term", evalMark, weight)), true));
            }


            return new Subject(true, evaluations, 0.0, 0.0, 0.0, 0.0, termWeight, culmWeight, 0, 0, 0, 0, totalCulmWeight, totalTermWeight);
        } else {

            // Parse weighting table

            Elements table = mainPage.select("div.green_border_message:nth-child(5) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1)").first().children();



            double kuWeighting = Double.parseDouble(table.get(1).select("td:nth-child(3)").text().replace("%", ""));
            double tWeighting = Double.parseDouble(table.get(2).select("td:nth-child(3)").text().replace("%", ""));
            double cWeighting = Double.parseDouble(table.get(3).select("td:nth-child(3)").text().replace("%", ""));
            double aWeighting = Double.parseDouble(table.get(4).select("td:nth-child(3)").text().replace("%", ""));
            double oWeighting = Double.parseDouble(table.get(5).select("td:nth-child(3)").text().replace("%", ""));
            double culmWeighting = Double.parseDouble(table.get(6).select("td:nth-child(2)").text().replace("%", ""));

            double totalKUWeight = 0.0;
            double totalTWeight = 0.0;
            double totalCWeight = 0.0;
            double totalAWeight = 0.0;
            double totalTermWeight = 0.0;
            double totalCulmWeight = 0.0;

            // As far as I know, other is never used, and its weight can be distributed amongst the other categories.

            kuWeighting += oWeighting / 4;
            tWeighting += oWeighting / 4;
            cWeighting += oWeighting / 4;
            aWeighting += oWeighting / 4;

            Elements evaluationList = mainPage.select("div.green_border_message:nth-child(3) > div:nth-child(1) > div:nth-child(3) > table:nth-child(1) > tbody:nth-child(1)").first().children();

            evaluationList.removeIf(e -> e.childrenSize() < 2);
            evaluationList.remove(0);

            List<Evaluation> evaluations = new ArrayList<>();

            for (Element eval : evaluationList) {
                String evalName = eval.select("td:nth-child(1)").get(0).text();


                //YandereDev-like mark finding. Uses the bgcolor attribute to tell between the different categories

                String kuMark = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("ffffaa")).findFirst().orElseGet(() -> new Element("p")).text().split("=")[0];
                double kuGrade;

                if (!kuMark.isEmpty()) {
                    kuGrade = Double.parseDouble(kuMark.split("/")[0]) / Double.parseDouble(kuMark.split("/")[1]) * 100;
                } else kuGrade = 0.0;

                String kuWeightString = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("ffffaa")).findFirst().orElseGet(() -> new Element("p")).text().replaceFirst("^.+?(?=%)", "").replace("% weight=", "");
                double kuWeight;

                if (!kuWeightString.isEmpty()) {
                    kuWeight = Double.parseDouble(kuWeightString);
                } else kuWeight = 0;

                String tMark = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("c0fea4")).findFirst().orElseGet(() -> new Element("p")).text().split("=")[0];
                double tGrade;

                if (!tMark.isEmpty()) {
                    tGrade = Double.parseDouble(tMark.split("/")[0]) / Double.parseDouble(tMark.split("/")[1]) * 100;
                } else tGrade = 0.0;

                String tWeightString = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("c0fea4")).findFirst().orElseGet(() -> new Element("p")).text().replaceFirst("^.+?(?=%)", "").replace("% weight=", "");
                double tWeight;

                if (!tWeightString.isEmpty()) {
                    tWeight = Double.parseDouble(tWeightString);
                } else tWeight = 0;

                String cMark = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("afafff")).findFirst().orElseGet(() -> new Element("p")).text().split("=")[0];
                double cGrade;

                if (!cMark.isEmpty()) {
                    cGrade = Double.parseDouble(cMark.split("/")[0]) / Double.parseDouble(cMark.split("/")[1]) * 100;
                } else cGrade = 0.0;

                String cWeightString = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("afafff")).findFirst().orElseGet(() -> new Element("p")).text().replaceFirst("^.+?(?=%)", "").replace("% weight=", "");
                double cWeight;

                if (!cWeightString.isEmpty()) {
                    cWeight = Double.parseDouble(cWeightString);
                } else cWeight = 0;

                String aMark = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("ffd490")).findFirst().orElseGet(() -> new Element("p")).text().split("=")[0];
                double aGrade;

                if (!aMark.isEmpty()) {
                    aGrade = Double.parseDouble(aMark.split("/")[0]) / Double.parseDouble(aMark.split("/")[1]) * 100;
                } else aGrade = 0.0;

                String aWeightString = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("ffd490")).findFirst().orElseGet(() -> new Element("p")).text().replaceFirst("^.+?(?=%)", "").replace("% weight=", "");
                double aWeight;

                if (!aWeightString.isEmpty()) {
                    aWeight = Double.parseDouble(aWeightString);
                } else aWeight = 0;

                String culmMark = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("dedede")).findFirst().orElseGet(() -> new Element("p")).text().split("=")[0];
                double culmGrade;

                if (!culmMark.isEmpty()) {
                    culmGrade = Double.parseDouble(culmMark.split("/")[0]) / Double.parseDouble(culmMark.split("/")[1]) * 100;
                } else culmGrade = 0.0;

                String culmWeightString = eval.select("td:nth-child(1)").stream().filter(e -> e.attr("bgcolor").equals("dedede")).findFirst().orElseGet(() -> new Element("p")).text().replaceFirst("^.+?(?=%)", "").replace("% weight=", "");
                double culmWeight;

                if (!culmWeightString.isEmpty()) {
                    culmWeight = Double.parseDouble(culmWeightString);
                } else culmWeight = 0;

                totalKUWeight += kuWeight;
                totalTWeight += tWeight;
                totalCWeight += cWeight;
                totalAWeight += aWeight;
                totalTermWeight += (totalKUWeight + totalTWeight + totalCWeight + totalAWeight);
                totalCulmWeight += culmWeight;

                evaluations.add(new Evaluation(evalName, List.of(new Evaluation.Category("K/U", kuGrade, kuWeight), new Evaluation.Category("T", tGrade, tWeight), new Evaluation.Category("C", cGrade, cWeight), new Evaluation.Category("A", aGrade, aWeight), new Evaluation.Category("Culm", culmGrade, culmWeight)), false));

            }




            return new Subject(false, evaluations, kuWeighting, tWeighting, cWeighting, aWeighting, kuWeighting + tWeighting + cWeighting + aWeighting, culmWeighting, totalKUWeight, totalTWeight, totalCWeight, totalAWeight, totalCulmWeight, totalTermWeight);
        }
    }
}
