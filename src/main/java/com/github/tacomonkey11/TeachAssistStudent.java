package com.github.tacomonkey11;

import com.github.tacomonkey11.model.Course;
import com.github.tacomonkey11.model.Evaluation;
import com.github.tacomonkey11.model.Subject;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TeachAssistStudent {
    public static final String BASE_URL = "https://ta.yrdsb.ca";
    private final OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(false).build();
    private final String username;
    private final String password;
    private String studentId;
    private LocalDateTime expirationDate;

    private String sessionToken;


    public TeachAssistStudent(String username, String password) throws IOException {
        this.username = username;
        this.password = password;

        if (!isAuthenticated())
            authenticate(username, password);
    }

    public void authenticate(String username, String password) throws IOException {
        Request request = new Request.Builder()
                .post(RequestBody.create("subjectid=0&username=" + username + "&password=" + password + "&submit=Login", MediaType.get("application/x-www-form-urlencoded")))
                .url(BASE_URL + "/yrdsb/index.php")
                .build();

        Response response = client.newCall(request).execute();
        this.expirationDate = LocalDateTime.now().plusMinutes(10);

        this.studentId = response.headers("Set-Cookie").stream().filter(s -> !s.contains("deleted")).collect(Collectors.toList()).get(1).replaceFirst(";.+$", "").replace("student_id=", "");
        this.sessionToken = response.headers("Set-Cookie").stream().filter(s -> !s.contains("deleted")).collect(Collectors.toList()).get(0).replaceFirst(";.+$", "").replace("session_token=", "");
    }

    private boolean isAuthenticated() {
        return expirationDate != null && expirationDate.isAfter(LocalDateTime.now());
    }

    public List<Course> getCourses() throws IOException {
        if (!isAuthenticated())
            authenticate(this.username, this.password);


        Request request = new Request.Builder()
                .get()
                .url(BASE_URL + "/live/students/listReports.php?student_id=" + studentId)
                .header("Cookie", "session_token=" + this.sessionToken + "; student_id=" + studentId)
                .build();

        return TeachAssistParser.parseCourses(Objects.requireNonNull(client.newCall(request).execute().body()).string());
    }

    public Subject getCourse(String subjectId, boolean v4) throws IOException {
        if (!isAuthenticated())
            authenticate(this.username, this.password);

        Request request;
        if (v4) {
            request = new Request.Builder()
                    .get()
                    .url(BASE_URL + "/live/students/viewReportOE.php?subject_id=" + subjectId + "&student_id=" + studentId)
                    .header("Cookie", "session_token=" + this.sessionToken + "; student_id=" + studentId)
                    .build();

        } else {
            request = new Request.Builder()
                    .get()
                    .url(BASE_URL + "/live/students/viewReport.php?subject_id=" + subjectId + "&student_id=" + studentId)
                    .header("Cookie", "session_token=" + this.sessionToken + "; student_id=" + studentId)
                    .build();

        }
        return TeachAssistParser.parseSubject(Objects.requireNonNull(client.newCall(request).execute().body()).string(), v4);
    }

    public double calculateCourseAverage(Subject subject) throws IOException {
        if (!isAuthenticated())
            authenticate(this.username, this.password);

        double totalKUMark = 0.0;
        double totalTMark = 0.0;
        double totalCMark = 0.0;
        double totalAMark = 0.0;
        double totalTermMark = 0.0;
        double totalCulmMark = 0.0;


        for (Evaluation evaluation : subject.evaluations()) {
            if (!evaluation.v4()) {
                for (Evaluation.Category category : evaluation.categories()) {
                    double product = category.mark() * category.weight();
                    switch (category.name()) {
                        case "K/U" -> totalKUMark += product;
                        case "T" -> totalTMark += product;
                        case "C" -> totalCMark += product;
                        case "A" -> totalAMark += product;
                        case "Culm" -> totalCulmMark += product;
                    }
                }
            } else {
                for (Evaluation.Category category : evaluation.categories()) {
                    double product = category.mark() * category.weight();
                    switch (category.name()) {
                        case "Term" -> totalTermMark += product;
                        case "Culm" -> totalCulmMark += product;
                    }
                }
            }
        }

        if (!subject.v4()) {
            totalKUMark /= subject.totalKUWeights();
            totalTMark /= subject.totalTWeights();
            totalCMark /= subject.totalCWeights();
            totalAMark /= subject.totalAWeights();
            HashMap<String, List<Double>> markMap = new HashMap<>();
            markMap.put("K/U", List.of(totalKUMark, subject.kuWeighting()));
            markMap.put("T", List.of(totalTMark, subject.tWeighting()));
            markMap.put("C" , List.of(totalCMark, subject.cWeighting()));
            markMap.put("A", List.of(totalAMark, subject.aWeighting()));

            markMap.values().removeIf(m -> m.get(0).isNaN());

            totalTermMark = 0.0;

            for (List<Double> m : markMap.values()) {
                totalTermMark += m.get(0) * 100 / markMap.values().size();
            }

            totalTermMark /= 100;
            totalCulmMark /= subject.totalCulmWeights();
        }

        if (subject.totalCulmWeights() == 0) {
            return totalTermMark;
        }
        return (totalTermMark * subject.termWeighting() + totalCulmMark * subject.culmWeighting()) / 100;
    }

    public double calculateOverallAverage() throws IOException {
        if (!isAuthenticated())
            authenticate(this.username, this.password);

        List<Course> courses = getCourses();
        int openCourses = 0;
        double average = 0.0;

        for (Course course : courses) {
            if (course.isOpen()) {
                openCourses++;
                average += calculateCourseAverage(getCourse(course.subjectId(), course.v4()));
            }
        }

        return average / openCourses;
    }
}