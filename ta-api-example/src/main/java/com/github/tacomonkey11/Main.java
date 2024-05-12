package com.github.tacomonkey11;

import com.github.tacomonkey11.model.Course;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        TeachAssistStudent student = new TeachAssistStudent(System.getenv("TA_USERNAME"), System.getenv("TA_PASSWORD"));

        System.out.println("STUDENT INFORMATION");
        System.out.println("---------------------------------\n");

        for (Course course : student.getCourses()) {
            System.out.println("COURSE INFORMATION FOR " + course.name() + " (" + course.courseCode() + ")");
            System.out.println("Open Status: " + course.isOpen());
            System.out.println("---------------------------------");

            if (course.isOpen()) {
                System.out.println("COURSE AVERAGE: " + student.calculateCourseAverage(student.getCourse(course.subjectId(), course.v4())));
            }
            System.out.println("\n\n");
        }

        System.out.println("OVERALL AVERAGE: " + student.calculateOverallAverage());
    }
}