import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PersonalTaskManager {

    private static final String DB_FILE_PATH = "tasks_database.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> VALID_PRIORITIES = new HashSet<>();

    static {
        VALID_PRIORITIES.add("Thấp");
        VALID_PRIORITIES.add("Trung bình");
        VALID_PRIORITIES.add("Cao");
    }

    public JSONObject addNewTask(String title, String description,
                                 String dueDateStr, String priorityLevel) {
        if (isEmpty(title)) {
            logError("Lỗi: Tiêu đề không được để trống.");
            return null;
        }

        if (isEmpty(dueDateStr)) {
            logError("Lỗi: Ngày đến hạn không được để trống.");
            return null;
        }

        LocalDate dueDate = parseDueDate(dueDateStr);
        if (dueDate == null) return null;

        if (!VALID_PRIORITIES.contains(priorityLevel)) {
            logError("Lỗi: Mức độ ưu tiên không hợp lệ. Vui lòng chọn từ: Thấp, Trung bình, Cao.");
            return null;
        }

        JSONArray tasks = TaskRepository.loadTasksFromDb();

        if (isDuplicateTask(tasks, title, dueDate)) {
            logError(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
            return null;
        }

        JSONObject newTask = buildTask(title, description, dueDate, priorityLevel);
        tasks.add(newTask);

        TaskRepository.saveTasksToDb(tasks);

        System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %s", newTask.get("id")));
        return newTask;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void logError(String msg) {
        System.out.println(msg);
    }

    private LocalDate parseDueDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logError("Lỗi: Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
            return null;
        }
    }

    private boolean isDuplicateTask(JSONArray tasks, String title, LocalDate dueDate) {
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            if (task.get("title").toString().equalsIgnoreCase(title)
                    && task.get("due_date").toString().equals(dueDate.format(DATE_FORMATTER))) {
                return true;
            }
        }
        return false;
    }

    private JSONObject buildTask(String title, String description, LocalDate dueDate, String priority) {
        JSONObject task = new JSONObject();
        task.put("id", UUID.randomUUID().toString());
        task.put("title", title);
        task.put("description", description);
        task.put("due_date", dueDate.format(DATE_FORMATTER));
        task.put("priority", priority);
        task.put("status", "Chưa hoàn thành");
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        task.put("created_at", now);
        task.put("last_updated_at", now);
        return task;
    }

    public static void main(String[] args) {
        PersonalTaskManager manager = new PersonalTaskManager();
        manager.addNewTask("Mua sách", "Sách Công nghệ phần mềm", "2025-07-20", "Cao");
        manager.addNewTask("Tập thể dục", "Chạy bộ 30 phút", "2025-07-21", "Trung bình");
        manager.addNewTask("", "Nhiệm vụ không có tiêu đề", "2025-07-22", "Thấp");
    }
}

class TaskRepository {
    private static final String DB_FILE_PATH = "tasks_database.json";

    public static JSONArray loadTasksFromDb() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(DB_FILE_PATH)) {
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (IOException | ParseException e) {
            System.err.println("Lỗi khi đọc file database: " + e.getMessage());
        }
        return new JSONArray();
    }

    public static void saveTasksToDb(JSONArray tasksData) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) {
            file.write(tasksData.toJSONString());
            file.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage());
        }
    }
}
