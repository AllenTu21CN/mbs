package sanp.mp100.integration;

import java.util.List;
import java.util.Map;

/**
 * Created by Tuyj on 2017/10/25.
 */

public class BusinessPlatform {

    public class Province {
        public String id;
        public String province;
    }

    public List<Province> getAreaProvinces() {
        final String procedureName = "area.getProvinces";
        return null;
    }


    public class City {
        public String id;
        public String province;
        public String city;
    }

    public List<City> getAreaCitiesByProvince(String province) {
        final String procedureName = "area.getCitiesByProvince";
        return null;
    }


    public class District {
        public String id;
        public String province;
        public String city;
        public String district;
    }

    public List<District> getAreaDistrictsByCity(String province, String city) {
        final String procedureName = "area.getDistrictsByCity";
        return null;
    }


    public class School {
        public String id;
        public String name;
        public String type;
        public String role;
    }

    public List<School> getOrgSchoolsByArea(String province, String city, String district) {
        final String procedureName = "org.getSchoolsByArea";
        return null;
    }


    public class SchoolClass {
        public String id;
        public String name;
        public String type;
    }

    public List<SchoolClass> getOrgClassesBySchoolId(long school_id) {
        final String procedureName = "org.getClassesBySchoolId";
        return null;
    }


    public class TimeTable {
        public String id;
        public String type;
        public String subject_id;
        public String subject_name;
        public String title;
        public String teacher_id;
        public String teacher_name;
        public String date;
        public String section;
        public String duration;
        public String status;

    }

    public List<TimeTable> getLessonTimetable(long class_id, String start_date, String end_date) {
        final String procedureName = "lesson.getTimetable";
        return null;
    }

}
