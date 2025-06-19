package com.cvmmk

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "ConstructionDB"
        private const val DATABASE_VERSION = 6

        // Project table
        private const val TABLE_PROJECTS = "projects"
        private const val KEY_PROJECT_ID = "id"
        private const val KEY_PROJECT_NAME = "name"
        private const val KEY_PROJECT_LOCATION = "location"
        private const val KEY_PROJECT_START_DATE = "start_date"
        private const val KEY_PROJECT_STATUS = "status"
        private const val KEY_PROJECT_PROGRESS = "progress_percentage"
        private const val KEY_PROJECT_NOTES = "notes"

        // Worker table
        private const val TABLE_WORKERS = "workers"
        private const val KEY_WORKER_ID = "id"
        private const val KEY_WORKER_NAME = "name"
        private const val KEY_WORKER_ROLE = "role"

        // Assignment table
        const val TABLE_PROJECT_ASSIGNMENTS = "project_assignments"
        private const val KEY_ASSIGNMENT_ID = "id"
        private const val KEY_ASSIGNMENT_PROJECT_ID = "project_id"
        private const val KEY_ASSIGNMENT_WORKER_ID = "worker_id"

        // Users table
        private const val TABLE_USERS = "users"
        private const val KEY_USER_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ROLE = "role"
        private const val KEY_WORKER_ID_FK = "worker_id"

        // Project images table
        const val TABLE_PROJECT_IMAGES = "project_images"
        const val KEY_IMAGE_ID = "id"
        const val KEY_IMAGE_PROJECT_ID = "project_id"
        const val KEY_IMAGE_PATH = "image_path"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create projects table
        val createProjectsTable = """
            CREATE TABLE $TABLE_PROJECTS (
                $KEY_PROJECT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_PROJECT_NAME TEXT,
                $KEY_PROJECT_LOCATION TEXT,
                $KEY_PROJECT_START_DATE TEXT,
                $KEY_PROJECT_STATUS TEXT,
                $KEY_PROJECT_PROGRESS INTEGER,
                $KEY_PROJECT_NOTES TEXT
            )
        """.trimIndent()

        // Create workers table
        val createWorkersTable = """
            CREATE TABLE $TABLE_WORKERS (
                $KEY_WORKER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_WORKER_NAME TEXT,
                $KEY_WORKER_ROLE TEXT
            )
        """.trimIndent()

        // Create assignments table
        val createAssignmentsTable = """
            CREATE TABLE $TABLE_PROJECT_ASSIGNMENTS (
                $KEY_ASSIGNMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_ASSIGNMENT_PROJECT_ID INTEGER,
                $KEY_ASSIGNMENT_WORKER_ID INTEGER,
                FOREIGN KEY ($KEY_ASSIGNMENT_PROJECT_ID) REFERENCES $TABLE_PROJECTS($KEY_PROJECT_ID) ON DELETE CASCADE,
                FOREIGN KEY ($KEY_ASSIGNMENT_WORKER_ID) REFERENCES $TABLE_WORKERS($KEY_WORKER_ID)
            )
        """.trimIndent()

        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $KEY_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USERNAME TEXT UNIQUE,
                $KEY_PASSWORD TEXT,
                $KEY_ROLE TEXT,
                $KEY_WORKER_ID_FK INTEGER,
                FOREIGN KEY ($KEY_WORKER_ID_FK) REFERENCES $TABLE_WORKERS($KEY_WORKER_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        // Create project images table
        val createProjectImagesTable = """
            CREATE TABLE $TABLE_PROJECT_IMAGES (
                $KEY_IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_IMAGE_PROJECT_ID INTEGER,
                $KEY_IMAGE_PATH TEXT,
                FOREIGN KEY ($KEY_IMAGE_PROJECT_ID) REFERENCES $TABLE_PROJECTS($KEY_PROJECT_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createProjectsTable)
        db.execSQL(createWorkersTable)
        db.execSQL(createAssignmentsTable)
        db.execSQL(createUsersTable)
        db.execSQL(createProjectImagesTable)

        // Pre-populate sample users
        val admin1 = ContentValues().apply {
            put(KEY_USERNAME, "Admin")
            put(KEY_PASSWORD, "admin123")
            put(KEY_ROLE, "admin")
            putNull(KEY_WORKER_ID_FK)
        }
        db.insert(TABLE_USERS, null, admin1)

        val admin2 = ContentValues().apply {
            put(KEY_USERNAME, "Wahyu")
            put(KEY_PASSWORD, "123456")
            put(KEY_ROLE, "admin")
            putNull(KEY_WORKER_ID_FK)
        }
        db.insert(TABLE_USERS, null, admin2)

        // Log created tables
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    Log.d("DatabaseHelper", "Table: ${cursor.getString(0)}")
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROJECT_IMAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROJECT_ASSIGNMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROJECTS")
        onCreate(db)
    }

    // Add Project
    fun addProject(
        name: String,
        location: String,
        startDate: String,
        status: String,
        progressPercentage: Int,
        notes: String?
    ): Long {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(KEY_PROJECT_NAME, name)
                put(KEY_PROJECT_LOCATION, location)
                put(KEY_PROJECT_START_DATE, startDate)
                put(KEY_PROJECT_STATUS, status)
                put(KEY_PROJECT_PROGRESS, progressPercentage)
                put(KEY_PROJECT_NOTES, notes)
            }
            val projectId = db.insert(TABLE_PROJECTS, null, values)
            Log.d("DatabaseHelper", "Project added with ID: $projectId")
            projectId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding project: ${e.message}", e)
            -1
        }
    }

    // Add Project Image
    fun addProjectImage(projectId: Int, imagePath: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_IMAGE_PROJECT_ID, projectId)
            put(KEY_IMAGE_PATH, imagePath)
        }
        return try {
            val id = db.insert(TABLE_PROJECT_IMAGES, null, values)
            Log.d("DatabaseHelper", "Image added for project $projectId: $imagePath")
            id
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding project image: ${e.message}", e)
            -1
        }
    }

    // Get Project Images
    fun getProjectImages(projectId: Int): List<String> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PROJECT_IMAGES,
            arrayOf(KEY_IMAGE_PATH),
            "$KEY_IMAGE_PROJECT_ID = ?",
            arrayOf(projectId.toString()),
            null, null, null
        )
        val imagePaths = mutableListOf<String>()
        try {
            while (cursor.moveToNext()) {
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMAGE_PATH))?.let {
                    imagePaths.add(it)
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting project images: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return imagePaths
    }

    // Get Dashboard Stats
    fun getDashboardStats(): DashboardStats {
        val db = readableDatabase
        var totalProjects = 0
        var activeProjects = 0
        var totalWorkers = 0
        var totalAssignments = 0

        try {
            // Total projects
            val projectCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PROJECTS", null)
            if (projectCursor.moveToFirst()) {
                totalProjects = projectCursor.getInt(0)
            }
            projectCursor.close()
            Log.d("DatabaseHelper", "Total projects: $totalProjects")

            // Active projects
            val activeProjectCursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_PROJECTS WHERE $KEY_PROJECT_STATUS != ?",
                arrayOf("Selesai")
            )
            if (activeProjectCursor.moveToFirst()) {
                activeProjects = activeProjectCursor.getInt(0)
            }
            activeProjectCursor.close()
            Log.d("DatabaseHelper", "Active projects: $activeProjects")

            // Total workers
            val workerCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_WORKERS", null)
            if (workerCursor.moveToFirst()) {
                totalWorkers = workerCursor.getInt(0)
            }
            workerCursor.close()
            Log.d("DatabaseHelper", "Total workers: $totalWorkers")

            // Total assignments
            val assignmentCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PROJECT_ASSIGNMENTS", null)
            if (assignmentCursor.moveToFirst()) {
                totalAssignments = assignmentCursor.getInt(0)
            }
            assignmentCursor.close()
            Log.d("DatabaseHelper", "Total assignments: $totalAssignments")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting dashboard stats: ${e.message}", e)
        }

        return DashboardStats(totalProjects, activeProjects, totalWorkers, totalAssignments)
    }

    // Get Active Project Count
    fun getActiveProjectCount(): Int {
        val db = readableDatabase
        var count = 0
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_PROJECTS WHERE $KEY_PROJECT_STATUS != ?",
                arrayOf("Selesai")
            )
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            Log.d("DatabaseHelper", "Active project count: $count")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting active project count: ${e.message}", e)
        }
        return count
    }

    // Get Worker Count
    fun getWorkerCount(): Int {
        val db = readableDatabase
        var count = 0
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_WORKERS", null)
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            Log.d("DatabaseHelper", "Worker count: $count")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting worker count: ${e.message}", e)
        }
        return count
    }

    // Update Project Progress
    fun updateProjectProgress(projectId: Int, percentage: Int, notes: String, imagePath: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_PROJECT_PROGRESS, percentage)
            put(KEY_PROJECT_NOTES, notes)
            val status = when (percentage) {
                0 -> "Belum Dimulai"
                in 1..99 -> "Sedang Berjalan"
                100 -> "Selesai"
                else -> "Belum Dimulai"
            }
            put(KEY_PROJECT_STATUS, status)
        }
        return try {
            val rows = db.update(TABLE_PROJECTS, values, "$KEY_PROJECT_ID = ?", arrayOf(projectId.toString()))
            Log.d("DatabaseHelper", "Updated $rows project rows for progress, projectId=$projectId")
            rows
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating project progress: ${e.message}", e)
            0
        }
    }

    // Get All Projects
    fun getAllProjects(): List<Project> {
        val projectList = mutableListOf<Project>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PROJECTS", null)

        try {
            if (cursor.moveToFirst()) {
                do {
                    val projectId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_ID))
                    val imagePaths = getProjectImages(projectId)
                    val project = Project(
                        id = projectId,
                        name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NAME)),
                        location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_LOCATION)),
                        startDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_START_DATE)),
                        status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_STATUS)) ?: "Belum Dimulai",
                        progressPercentage = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_PROGRESS)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NOTES)),
                        imagePaths = imagePaths
                    )
                    projectList.add(project)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting all projects: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return projectList
    }

    // Authenticate User
    fun authenticateUser(username: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $KEY_USERNAME = ? AND $KEY_PASSWORD = ?",
            arrayOf(username, password)
        )
        return try {
            if (cursor.moveToFirst()) {
                val user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                    workerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_WORKER_ID_FK))
                )
                cursor.close()
                user
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error authenticating user: ${e.message}", e)
            cursor.close()
            null
        }
    }

    // Add User
    fun addUser(username: String, password: String, role: String, workerId: Int? = null): Long {
        val db = writableDatabase
        if (isUsernameTaken(username, -1)) {
            Log.w("DatabaseHelper", "Username $username already exists")
            return -1
        }
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
            put(KEY_ROLE, role)
            if (workerId != null) {
                put(KEY_WORKER_ID_FK, workerId)
            } else {
                putNull(KEY_WORKER_ID_FK)
            }
        }
        return try {
            val id = db.insert(TABLE_USERS, null, values)
            Log.d("DatabaseHelper", "User added with ID: $id, username=$username, role=$role, workerId=$workerId")
            id
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding user: ${e.message}", e)
            -1
        }
    }

    // Get Worker Accounts
    fun getWorkerAccounts(): List<WorkerAccount> {
        val accountList = mutableListOf<WorkerAccount>()
        val db = readableDatabase
        val query = """
            SELECT u.$KEY_USER_ID, u.$KEY_USERNAME, u.$KEY_PASSWORD, u.$KEY_ROLE, 
                   w.$KEY_WORKER_ID, w.$KEY_WORKER_NAME, w.$KEY_WORKER_ROLE
            FROM $TABLE_USERS u
            LEFT JOIN $TABLE_WORKERS w ON u.$KEY_WORKER_ID_FK = w.$KEY_WORKER_ID
            WHERE u.$KEY_ROLE = 'worker'
        """.trimIndent()
        val cursor = db.rawQuery(query, null)

        try {
            if (cursor.moveToFirst()) {
                do {
                    val account = WorkerAccount(
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                        username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                        password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                        role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                        workerId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WORKER_ID)),
                        workerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_NAME)),
                        workerRole = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_ROLE))
                    )
                    accountList.add(account)
                } while (cursor.moveToNext())
            }
            Log.d("DatabaseHelper", "Loaded ${accountList.size} worker accounts")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting worker accounts: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return accountList
    }

    // Update User Account
    fun updateUserAccount(userId: Int, username: String, password: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
        }
        return try {
            val rows = db.update(TABLE_USERS, values, "$KEY_USER_ID = ?", arrayOf(userId.toString()))
            Log.d("DatabaseHelper", "Updated $rows rows for userId=$userId")
            rows
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Update failed: ${e.message}", e)
            0
        }
    }

    // Update Worker
    fun updateWorker(workerId: Int, name: String, role: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_WORKER_NAME, name)
            put(KEY_WORKER_ROLE, role)
        }
        return try {
            val rows = db.update(TABLE_WORKERS, values, "$KEY_WORKER_ID = ?", arrayOf(workerId.toString()))
            Log.d("DatabaseHelper", "Updated $rows worker rows for workerId=$workerId")
            rows
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating worker: ${e.message}", e)
            0
        }
    }

    // Delete Worker Account
    fun deleteWorkerAccount(userId: Int, workerId: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val userDeleted = db.delete(TABLE_USERS, "$KEY_USER_ID = ?", arrayOf(userId.toString()))
            val workerDeleted = db.delete(TABLE_WORKERS, "$KEY_WORKER_ID = ?", arrayOf(workerId.toString()))
            if (userDeleted > 0 && workerDeleted > 0) {
                db.setTransactionSuccessful()
                Log.d("DatabaseHelper", "Deleted userId=$userId, workerId=$workerId")
                true
            } else {
                Log.e("DatabaseHelper", "Delete failed: user=$userDeleted, worker=$workerDeleted")
                false
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Delete error: ${e.message}", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    // Delete Project
    fun deleteProject(id: Int): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = db.delete(TABLE_PROJECTS, "$KEY_PROJECT_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            Log.d("DatabaseHelper", "Deleted $result project rows for id=$id")
            return result
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting project: ${e.message}", e)
            return 0
        } finally {
            db.endTransaction()
        }
    }

    // Add Worker
    fun addWorker(name: String, role: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_WORKER_NAME, name)
            put(KEY_WORKER_ROLE, role)
        }
        return try {
            val id = db.insert(TABLE_WORKERS, null, values)
            Log.d("DatabaseHelper", "Worker added with ID: $id, name=$name, role=$role")
            id
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding worker: ${e.message}", e)
            -1
        }
    }

    // Check if Username is Taken
    fun isUsernameTaken(username: String, excludeUserId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $KEY_USERNAME = ? AND $KEY_USER_ID != ?",
            arrayOf(username, excludeUserId.toString())
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count > 0
    }

    // Get All Workers
    fun getAllWorkers(): List<Worker> {
        val workers = mutableListOf<Worker>()
        val db = readableDatabase
        val query = "SELECT $KEY_WORKER_ID, $KEY_WORKER_NAME, $KEY_WORKER_ROLE FROM $TABLE_WORKERS"
        val cursor = db.rawQuery(query, null)
        try {
            Log.d("DatabaseHelper", "Executing getAllWorkers query: $query")
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WORKER_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_NAME)) ?: ""
                val role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_ROLE))
                Log.d("DatabaseHelper", "Worker found: id=$id, name=$name, role=$role")
                if (name.isNotEmpty()) {
                    workers.add(Worker(id = id, name = name, role = role))
                }
            }
            Log.d("DatabaseHelper", "Total workers retrieved: ${workers.size}")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting workers: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return workers
    }

    // Get Worker by ID
    fun getWorkerById(workerId: Int): Worker? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_WORKERS WHERE $KEY_WORKER_ID = ?", arrayOf(workerId.toString()))
        return try {
            if (cursor.moveToFirst()) {
                val worker = Worker(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WORKER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_NAME)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_ROLE))
                )
                cursor.close()
                worker
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting worker by ID: ${e.message}", e)
            cursor.close()
            null
        }
    }

    // Get Worker Name by ID
    fun getWorkerName(workerId: Int): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WORKERS,
            arrayOf(KEY_WORKER_NAME),
            "$KEY_WORKER_ID = ?",
            arrayOf(workerId.toString()),
            null, null, null
        )
        return try {
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_NAME))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting worker name: ${e.message}", e)
            null
        } finally {
            cursor.close()
        }
    }

    // Assign Worker to Project
    fun assignWorkerToProject(workerId: Int, projectId: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_ASSIGNMENT_WORKER_ID, workerId)
            put(KEY_ASSIGNMENT_PROJECT_ID, projectId)
        }
        return try {
            val id = db.insert(TABLE_PROJECT_ASSIGNMENTS, null, values)
            Log.d("DatabaseHelper", "Assigned worker $workerId to project $projectId, assignment ID: $id")
            id
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error assigning worker to project: ${e.message}", e)
            -1
        }
    }

    // Get Projects with Full Details
    fun getProjectsForWorker(workerId: Int): List<ProjectWithDetails> {
        val projects = mutableListOf<ProjectWithDetails>()
        val db = readableDatabase
        val query = """
            SELECT p.*, GROUP_CONCAT(pa.$KEY_ASSIGNMENT_WORKER_ID) as worker_ids
            FROM $TABLE_PROJECTS p
            LEFT JOIN $TABLE_PROJECT_ASSIGNMENTS pa ON p.$KEY_PROJECT_ID = pa.$KEY_ASSIGNMENT_PROJECT_ID
            WHERE pa.$KEY_ASSIGNMENT_WORKER_ID = ?
            GROUP BY p.$KEY_PROJECT_ID
        """
        val cursor = db.rawQuery(query, arrayOf(workerId.toString()))

        try {
            while (cursor.moveToNext()) {
                val projectId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_ID))
                val workerIds = cursor.getString(cursor.getColumnIndexOrThrow("worker_ids"))?.split(",") ?: emptyList()
                val workerNames = workerIds.mapNotNull { wid ->
                    getWorkerName(wid.toIntOrNull() ?: return@mapNotNull null)
                }.joinToString(", ")

                val project = ProjectWithDetails(
                    id = projectId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NAME)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_LOCATION)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_START_DATE)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_STATUS)) ?: "Belum Dimulai",
                    progressPercentage = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_PROGRESS)),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NOTES)),
                    workerName = workerNames.ifEmpty { "-" },
                    imagePaths = getProjectImages(projectId)
                )
                projects.add(project)
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting worker projects: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return projects
    }

    // Get Assignment IDs for Project
    fun getAssignmentIdsForProject(projectId: Int): List<Int> {
        val assignmentIds = mutableListOf<Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $KEY_ASSIGNMENT_ID FROM $TABLE_PROJECT_ASSIGNMENTS WHERE $KEY_ASSIGNMENT_PROJECT_ID = ?",
            arrayOf(projectId.toString())
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    assignmentIds.add(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ASSIGNMENT_ID)))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting assignment IDs: ${e.message}", e)
        } finally {
            cursor.close()
        }
        return assignmentIds
    }

    // Get Project by ID
    fun getProjectById(projectId: Int): Project? {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_PROJECTS WHERE $KEY_PROJECT_ID = ?
        """
        val cursor = db.rawQuery(query, arrayOf(projectId.toString()))
        return try {
            if (cursor.moveToFirst()) {
                Project(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NAME)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_LOCATION)),
                    startDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_START_DATE)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_STATUS)) ?: "Belum Dimulai",
                    progressPercentage = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROJECT_PROGRESS)),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROJECT_NOTES)),
                    imagePaths = getProjectImages(projectId)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting project by ID: ${e.message}", e)
            null
        } finally {
            cursor.close()
        }
    }

    // Update Project
    fun updateProject(
        projectId: Int,
        name: String,
        location: String,
        startDate: String,
        status: String,
        progressPercentage: Int,
        notes: String?
    ): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(KEY_PROJECT_NAME, name)
                put(KEY_PROJECT_LOCATION, location)
                put(KEY_PROJECT_START_DATE, startDate)
                put(KEY_PROJECT_STATUS, status)
                put(KEY_PROJECT_PROGRESS, progressPercentage)
                put(KEY_PROJECT_NOTES, notes)
            }
            val rows = db.update(
                TABLE_PROJECTS,
                values,
                "$KEY_PROJECT_ID = ?",
                arrayOf(projectId.toString())
            )
            Log.d("DatabaseHelper", "Updated $rows row(s) for project ID: $projectId")
            rows > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating project: ${e.message}", e)
            false
        }
    }

    fun debugWorkersTable() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_WORKERS", null)
        try {
            Log.d("DatabaseHelper", "Dumping workers table:")
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WORKER_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_NAME))
                val role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WORKER_ROLE))
                Log.d("DatabaseHelper", "Worker: id=$id, name=$name, role=$role")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error dumping workers: ${e.message}", e)
        } finally {
            cursor.close()
        }
    }

    // Delete Project Images
    fun deleteProjectImages(projectId: Int) {
        try {
            val db = writableDatabase
            val rows = db.delete(
                TABLE_PROJECT_IMAGES,
                "$KEY_IMAGE_PROJECT_ID = ?",
                arrayOf(projectId.toString())
            )
            Log.d("DatabaseHelper", "Deleted $rows image(s) for project ID: $projectId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting project images: ${e.message}", e)
        }
    }

    // Get User by ID
    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $KEY_USER_ID = ?",
            arrayOf(userId.toString())
        )
        return try {
            if (cursor.moveToFirst()) {
                val user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                    workerId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_WORKER_ID_FK))
                )
                cursor.close()
                user
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting user by ID: ${e.message}", e)
            cursor.close()
            null
        }
    }

    // Extension function for nullable Int
    private fun android.database.Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }
}

data class Project(
    val id: Int,
    val name: String,
    val location: String,
    val startDate: String,
    val status: String,
    val progressPercentage: Int,
    val notes: String?,
    val imagePaths: List<String>
)

data class ProjectWithDetails(
    val id: Int,
    val name: String?,
    val location: String?,
    val startDate: String?,
    val status: String?,
    val progressPercentage: Int,
    val notes: String?,
    val workerName: String?,
    val imagePaths: List<String>
)

data class Worker(
    val id: Int,
    val name: String,
    val role: String?
)

data class User(
    val id: Int,
    val username: String?,
    val role: String?,
    val workerId: Int?
)

data class WorkerAccount(
    val userId: Int,
    val username: String?,
    val password: String?,
    val role: String?,
    val workerId: Int,
    val workerName: String?,
    val workerRole: String?
)

data class DashboardStats(
    val totalProjects: Int,
    val activeProjects: Int,
    val totalWorkers: Int,
    val totalAssignments: Int
)