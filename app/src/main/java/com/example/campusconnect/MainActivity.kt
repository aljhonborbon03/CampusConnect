package com.example.campusconnect

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusconnect.data.model.Announcement
import com.example.campusconnect.data.model.ServiceRequest
import com.example.campusconnect.data.model.User
import com.example.campusconnect.data.model.Service
import com.example.campusconnect.data.repository.AnnouncementRepository
import com.example.campusconnect.data.repository.RequestRepository
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- UI Constants ---
val PrimaryBlueVal = Color(0xFF1A237E)
val SecondaryBlueVal = Color(0xFF3949AB)
val AccentOrangeVal = Color(0xFFFF9800)
val SuccessGreenVal = Color(0xFF4CAF50)
val BackgroundGrayVal = Color(0xFFF5F7FA)
val SurfaceWhiteVal = Color(0xFFFFFFFF)
val TextDarkVal = Color(0xFF212121)
val TextLightVal = Color(0xFF757575)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundGrayVal
                ) {
                    CampusConnectApp()
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlueVal,
            secondary = SecondaryBlueVal,
            tertiary = AccentOrangeVal,
            background = BackgroundGrayVal,
            surface = SurfaceWhiteVal
        ),
        content = content
    )
}

enum class Screen {
    SPLASH, LOGIN, REGISTER, DASHBOARD, ADMIN_DASHBOARD, REQUEST_FORM, MY_REQUESTS, STUDENT_REQUESTS, ANNOUNCEMENTS, CREATE_ANNOUNCEMENT, USERS, LOGOUT, PROFILE, NOTIFICATIONS, NOTIFICATION_DETAIL
}

@Composable
fun CampusConnectApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Repositories
    val userRepository = remember { UserRepository() }
    val requestRepository = remember { RequestRepository(context) }
    val announcementRepository = remember { AnnouncementRepository(context) }
    
    val auth = remember { FirebaseAuth.getInstance() }

    var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    
    // UI States
    var currentUserData by remember { mutableStateOf<User?>(null) }
    var allUsersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    
    val requestList by requestRepository.requests.collectAsState(initial = emptyList())
    val announcementList by announcementRepository.announcements.collectAsState(initial = emptyList())

    // Initial Data Loading
    LaunchedEffect(Unit) {
        val uid = userRepository.getCurrentUserUid()
        if (uid != null) {
            currentUserData = userRepository.getUserData(uid)
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.SPLASH) {
            delay(2500)
            if (auth.currentUser != null && currentUserData != null) {
                currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD
            } else {
                currentScreen = Screen.LOGIN
            }
        }
        
        if (currentScreen == Screen.USERS) {
            allUsersList = userRepository.getAllUsers()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGrayVal
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) + slideInHorizontally(initialOffsetX = { 1000 }))
                        .togetherWith(fadeOut(animationSpec = tween(500)) + slideOutHorizontally(targetOffsetX = { -1000 }))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.SPLASH -> SplashScreen()
                    Screen.LOGIN -> LoginScreen(
                        onLoginSuccess = { email, password -> 
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = auth.currentUser?.uid
                                        if (uid != null) {
                                            scope.launch {
                                                val user = userRepository.getUserData(uid)
                                                currentUserData = user
                                                if (user != null) {
                                                    currentScreen = if (user.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD
                                                } else {
                                                    Toast.makeText(context, "User profile not found", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER }
                    )
                    Screen.REGISTER -> RegisterScreen(
                        onRegisterSuccess = { name, email, password, role -> 
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = auth.currentUser?.uid
                                        if (uid != null) {
                                            scope.launch {
                                                try {
                                                    userRepository.createUser(uid, name, email, role)
                                                    Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                                                    currentScreen = Screen.LOGIN
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        onBackToLogin = { currentScreen = Screen.LOGIN }
                    )
                    Screen.DASHBOARD -> DashboardScreen(
                        user = currentUserData,
                        notifications = requestList.filter { it.requesterName == currentUserData?.name && it.status != "Pending" && !it.isRead },
                        onNavigateToRequest = { currentScreen = Screen.REQUEST_FORM },
                        onNavigateToMyRequests = { currentScreen = Screen.MY_REQUESTS },
                        onNavigateToAnnouncements = { currentScreen = Screen.ANNOUNCEMENTS },
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                1 -> Screen.MY_REQUESTS
                                2 -> Screen.ANNOUNCEMENTS
                                else -> Screen.DASHBOARD
                            }
                        },
                        onProfileClick = { currentScreen = Screen.PROFILE },
                        onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                        onLogoutClick = { currentScreen = Screen.LOGOUT }
                    )
                    Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                        user = currentUserData,
                        requestCount = requestList.size,
                        pendingCount = requestList.count { it.status == "Pending" },
                        userCount = allUsersList.size,
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                1 -> Screen.STUDENT_REQUESTS
                                2 -> Screen.ANNOUNCEMENTS
                                3 -> Screen.USERS
                                else -> Screen.ADMIN_DASHBOARD
                            }
                        },
                        onProfileClick = { currentScreen = Screen.PROFILE },
                        onLogoutClick = { currentScreen = Screen.LOGOUT },
                        onNotificationClick = { currentScreen = Screen.NOTIFICATIONS },
                        notificationCount = requestList.count { it.status == "Pending" }
                    )
                    Screen.REQUEST_FORM -> RequestFormScreen(
                        user = currentUserData,
                        onSubmitRequest = { name, id, service, desc ->
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())
                            
                            val newRequest = ServiceRequest(
                                id = UUID.randomUUID().toString(),
                                requesterName = name,
                                studentId = id,
                                service = service,
                                description = desc,
                                date = currentDate,
                                status = "Pending",
                                isRead = false
                            )
                            scope.launch {
                                requestRepository.addRequest(newRequest)
                                Toast.makeText(context, "Request Submitted Successfully", Toast.LENGTH_SHORT).show()
                                currentScreen = Screen.DASHBOARD
                            }
                        },
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                    Screen.MY_REQUESTS -> MyRequestsScreen(
                        requests = requestList.filter { it.requesterName == currentUserData?.name },
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                    Screen.STUDENT_REQUESTS -> StudentRequestsScreen(
                        requests = requestList,
                        onUpdateStatus = { requestId, newStatus ->
                            scope.launch {
                                requestRepository.updateRequestStatus(requestId, newStatus)
                            }
                        },
                        onUploadDocument = { requestId, uri ->
                            scope.launch {
                                try {
                                    Toast.makeText(context, "Uploading document...", Toast.LENGTH_SHORT).show()
                                    requestRepository.uploadDocument(requestId, uri)
                                    Toast.makeText(context, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onBack = { currentScreen = Screen.ADMIN_DASHBOARD }
                    )
                    Screen.ANNOUNCEMENTS -> AnnouncementsScreen(
                        announcements = announcementList,
                        isAdmin = currentUserData?.role == "Admin",
                        currentUserName = currentUserData?.name ?: "",
                        onAddAnnouncement = { currentScreen = Screen.CREATE_ANNOUNCEMENT },
                        onDeleteAnnouncement = { id ->
                            scope.launch {
                                announcementRepository.deleteAnnouncement(id)
                                Toast.makeText(context, "Announcement Deleted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onJoinEvent = { id ->
                            scope.launch {
                                announcementRepository.joinEvent(id, currentUserData?.name ?: "Student")
                                Toast.makeText(context, "Joined Event Successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBack = { 
                            currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD 
                        }
                    )
                    Screen.CREATE_ANNOUNCEMENT -> CreateAnnouncementScreen(
                        onAnnouncementCreated = { title, date, location, desc ->
                            val newAnnouncement = Announcement(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                date = date,
                                location = location,
                                description = desc,
                                participants = emptyList()
                            )
                            scope.launch {
                                announcementRepository.addAnnouncement(newAnnouncement)
                                Toast.makeText(context, "Announcement Published", Toast.LENGTH_SHORT).show()
                                currentScreen = Screen.ANNOUNCEMENTS
                            }
                        },
                        onBack = { currentScreen = Screen.ANNOUNCEMENTS }
                    )
                    Screen.USERS -> UserManagementScreen(
                        users = allUsersList,
                        onBack = { currentScreen = Screen.ADMIN_DASHBOARD }
                    )
                    Screen.PROFILE -> ProfileScreen(
                        user = currentUserData,
                        onBack = { 
                            currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD 
                        }
                    )
                    Screen.NOTIFICATIONS -> NotificationScreen(
                        notifications = if (currentUserData?.role == "Admin") {
                            requestList.filter { it.status == "Pending" }
                        } else {
                            requestList.filter { it.requesterName == currentUserData?.name && it.status != "Pending" && !it.isRead }
                        },
                        onNotificationClick = { request ->
                            selectedRequest = request
                            if (currentUserData?.role != "Admin") {
                                scope.launch {
                                    requestRepository.markAsRead(request.id)
                                }
                                currentScreen = Screen.NOTIFICATION_DETAIL
                            } else {
                                currentScreen = Screen.STUDENT_REQUESTS
                            }
                        },
                        onBack = { 
                            currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD 
                        }
                    )
                    Screen.NOTIFICATION_DETAIL -> NotificationDetailScreen(
                        request = selectedRequest,
                        onMarkAsRead = { requestId ->
                            scope.launch {
                                requestRepository.markAsRead(requestId)
                                currentScreen = Screen.NOTIFICATIONS
                            }
                        },
                        onBack = { currentScreen = Screen.NOTIFICATIONS }
                    )
                    Screen.LOGOUT -> {
                        LaunchedEffect(Unit) {
                            auth.signOut()
                            currentUserData = null
                            currentScreen = Screen.LOGIN
                        }
                    }
                }
            }
        }
    }
}

// --- Screens ---

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "Splash")
    val alphaValue by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(PrimaryBlueVal, SecondaryBlueVal))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoComponent(size = 120.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "CAMPUS CONNECT",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alphaValue)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bridging Students and Services",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoComponent(size = 80.dp, borderEnabled = true)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
        Text("Log in to your account", color = TextLightVal)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it }, 
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onLoginSuccess(email, password) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("LOGIN", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: (String, String, String, String) -> Unit, onBackToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Student") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Register as:", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = role == "Student", onClick = { role = "Student" })
            Text("Student")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = role == "Admin", onClick = { role = "Admin" })
            Text("Admin")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onRegisterSuccess(name, email, password, role) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("REGISTER", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Login")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(user: User?, notifications: List<ServiceRequest>, onNavigateToRequest: () -> Unit, onNavigateToMyRequests: () -> Unit, onNavigateToAnnouncements: () -> Unit, onTabClick: (Int) -> Unit, onProfileClick: () -> Unit, onNotificationClick: () -> Unit, onLogoutClick: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val services = listOf(
        Service("Document Request", "Official university transcripts and certifications.", Icons.Default.Description),
        Service("Counseling", "Confidential support for mental health and well-being.", Icons.Default.Person),
        Service("IT Support", "Assistance with network, accounts, and hardware.", Icons.Default.Computer)
    )
    
    val filteredServices = services.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(
            title = "Campus Connect", 
            welcomeText = "Hi, ${user?.name ?: "Student"}", 
            notificationCount = notifications.size,
            onLogout = onLogoutClick,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick
        )
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SecondaryTabRow(
                selectedTabIndex = 0,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlueVal,
                divider = {}
            ) {
                listOf("Services", "My Requests", "Announcements").forEachIndexed { index, title ->
                    Tab(
                        selected = index == 0,
                        onClick = { onTabClick(index) },
                        enabled = true,
                        text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardModern(
                    title = "My Requests", 
                    count = "View", 
                    icon = Icons.AutoMirrored.Filled.Assignment, 
                    color = PrimaryBlueVal, 
                    modifier = Modifier.weight(1f).clickable { onNavigateToMyRequests() }
                )
                StatCardModern(
                    title = "Announcements", 
                    count = "View", 
                    icon = Icons.Default.Campaign, 
                    color = AccentOrangeVal, 
                    modifier = Modifier.weight(1f).clickable { onNavigateToAnnouncements() }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            SearchBar(value = searchQuery, onValueChange = { searchQuery = it })
            Spacer(modifier = Modifier.height(24.dp))
            Text("All Services", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(filteredServices) { service -> 
                    ServiceCardModern(service = service, onClick = onNavigateToRequest) 
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(user: User?, requestCount: Int, pendingCount: Int, userCount: Int, onTabClick: (Int) -> Unit, onProfileClick: () -> Unit, onLogoutClick: () -> Unit, onNotificationClick: () -> Unit, notificationCount: Int) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundGrayVal)) {
        DashboardHeader(
            title = "Admin Panel", 
            welcomeText = "Admin: ${user?.name}", 
            onLogout = onLogoutClick, 
            onProfileClick = onProfileClick,
            isAdmin = true,
            onNotificationClick = onNotificationClick,
            notificationCount = notificationCount
        )
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SecondaryTabRow(selectedTabIndex = 0, containerColor = Color.Transparent, contentColor = PrimaryBlueVal, divider = {}) {
                listOf("Dashboard", "Requests", "Announcements", "Users").forEachIndexed { index, title ->
                    Tab(selected = index == 0, onClick = { onTabClick(index) }, enabled = true, text = { Text(title, fontSize = 12.sp) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardModern(title = "Total Requests", count = requestCount.toString(), icon = Icons.AutoMirrored.Filled.Assignment, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                StatCardModern(title = "Users", count = userCount.toString(), icon = Icons.Default.Group, color = PrimaryBlueVal, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentOrangeVal.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, AccentOrangeVal.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = AccentOrangeVal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("$pendingCount Pending Approval(s)", fontWeight = FontWeight.Bold, color = AccentOrangeVal)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onTabClick(1) }) { Text("Review All") }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Quick Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { AdminActionItem(title = "Review Student Submissions", icon = Icons.Default.PendingActions, onClick = { onTabClick(1) }) }
                item { AdminActionItem(title = "Upload Request Documents", icon = Icons.Default.CloudUpload, onClick = { onTabClick(1) }) }
                item { AdminActionItem(title = "Broadcast Announcement", icon = Icons.Default.Campaign, onClick = { onTabClick(2) }) }
                item { AdminActionItem(title = "User Management", icon = Icons.Default.ManageAccounts, onClick = { onTabClick(3) }) }
            }
        }
    }
}

@Composable
fun DashboardHeader(title: String, welcomeText: String, notificationCount: Int = 0, onLogout: () -> Unit, onProfileClick: () -> Unit, onNotificationClick: () -> Unit = {}, isAdmin: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isAdmin) Color(0xFFB71C1C) else PrimaryBlueVal,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable { onProfileClick() }) {
                LogoComponent(size = 48.dp, borderEnabled = false)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(welcomeText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            
            IconButton(onClick = onNotificationClick) {
                BadgedBox(badge = { if (notificationCount > 0) Badge { Text(notificationCount.toString()) } }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
            }
            
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
            }
        }
    }
}

@Composable
fun StatCardModern(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = TextLightVal, fontWeight = FontWeight.Medium)
            }
            Text(count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
        }
    }
}

@Composable
fun ServiceCardModern(service: Service, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(SecondaryBlueVal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(service.icon, contentDescription = null, tint = SecondaryBlueVal)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDarkVal)
                Text(service.description, fontSize = 12.sp, color = TextLightVal)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextLightVal)
        }
    }
}

@Composable
fun AdminActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlueVal)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
        placeholder = { Text("Search services...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhiteVal,
            unfocusedContainerColor = SurfaceWhiteVal,
            disabledContainerColor = SurfaceWhiteVal,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun LogoComponent(size: Dp, borderEnabled: Boolean = false) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White).then(if (borderEnabled) Modifier.background(PrimaryBlueVal, CircleShape).padding(2.dp).background(Color.White, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.School, contentDescription = "Logo", tint = PrimaryBlueVal, modifier = Modifier.size(size * 0.6f))
    }
}

// --- Specific Screens (Simplified for context) ---

@Composable
fun RequestFormScreen(user: User?, onSubmitRequest: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var studentId by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Document Request") }
    var description by remember { mutableStateOf("") }
    
    val services = listOf("Document Request", "Counseling", "IT Support")

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Service Request Form", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Personal Information", fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
        OutlinedTextField(value = user?.name ?: "", onValueChange = {}, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = false)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID Number") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Request Details", fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
        
        Text("Select Service Type", fontSize = 12.sp, color = TextLightVal, modifier = Modifier.padding(top = 8.dp))
        services.forEach { service ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedService = service }) {
                RadioButton(selected = selectedService == service, onClick = { selectedService = service })
                Text(service)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description / Reason") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSubmitRequest(user?.name ?: "Student", studentId, selectedService, description) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SUBMIT REQUEST")
        }
    }
}

@Composable
fun MyRequestsScreen(requests: List<ServiceRequest>, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("My Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No requests found", color = TextLightVal)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(requests) { request ->
                    RequestCard(request, onViewDocument = { request.documentUrl?.let { uriHandler.openUri(it) } })
                }
            }
        }
    }
}

@Composable
fun StudentRequestsScreen(requests: List<ServiceRequest>, onUpdateStatus: (String, String) -> Unit, onUploadDocument: (String, Uri) -> Unit, onBack: () -> Unit) {
    var selectedRequestId by remember { mutableStateOf<String?>(null) }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedRequestId?.let { id -> onUploadDocument(id, it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Manage Student Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requests) { request ->
                AdminRequestCard(
                    request = request, 
                    onUpdateStatus = onUpdateStatus,
                    onPickFile = { 
                        selectedRequestId = request.id
                        filePicker.launch("*/*")
                    }
                )
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    announcements: List<Announcement>,
    isAdmin: Boolean,
    currentUserName: String,
    onAddAnnouncement: () -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onJoinEvent: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = onAddAnnouncement, containerColor = AccentOrangeVal) {
                    Icon(Icons.Default.Add, contentDescription = "Add Announcement", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Announcements", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(announcements) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        isAdmin = isAdmin,
                        isJoined = announcement.participants.contains(currentUserName),
                        onJoin = { onJoinEvent(announcement.id) },
                        onDelete = { onDeleteAnnouncement(announcement.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateAnnouncementScreen(onAnnouncementCreated: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("New Announcement", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g., Oct 25, 2023)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(200.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onAnnouncementCreated(title, date, location, content) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("PUBLISH")
        }
    }
}

@Composable
fun UserManagementScreen(users: List<User>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("User Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(users) { user ->
                UserCard(user)
            }
        }
    }
}

@Composable
fun ProfileScreen(user: User?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(PrimaryBlueVal), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).padding(16.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                LogoComponent(size = 80.dp)
            }
        }
        
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Profile Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileInfoRow(label = "Full Name", value = user?.name ?: "N/A")
            ProfileInfoRow(label = "Email Address", value = user?.email ?: "N/A")
            ProfileInfoRow(label = "Account Role", value = user?.role ?: "N/A")
        }
    }
}

@Composable
fun NotificationScreen(notifications: List<ServiceRequest>, onNotificationClick: (ServiceRequest) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No new notifications", color = TextLightVal)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notifications) { notification ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNotificationClick(notification) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentOrangeVal))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Request Updated", fontWeight = FontWeight.Bold, color = TextDarkVal)
                                Text("Your ${notification.service} request is now ${notification.status}.", color = TextLightVal, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailScreen(request: ServiceRequest?, onMarkAsRead: (String) -> Unit, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    LaunchedEffect(request?.id) {
        request?.id?.let { onMarkAsRead(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Notification Detail", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (request != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Update for ${request.service}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlueVal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Current Status:", fontSize = 12.sp, color = TextLightVal)
                    StatusBadge(request.status)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Message:", fontSize = 12.sp, color = TextLightVal)
                    Text("Your request for ${request.service} has been updated to ${request.status}.", fontSize = 15.sp, color = TextDarkVal)
                    
                    if (request.documentUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Result Document:", fontSize = 12.sp, color = TextLightVal)
                        Button(
                            onClick = { uriHandler.openUri(request.documentUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenVal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View / Download Document")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Request Description:", fontSize = 12.sp, color = TextLightVal)
                    Text(request.description, fontSize = 14.sp, color = TextDarkVal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Date Submitted: ${request.date}", fontSize = 12.sp, color = TextLightVal)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No detail available", color = TextLightVal)
            }
        }
    }
}

// --- Card Components ---

@Composable
fun RequestCard(request: ServiceRequest, onViewDocument: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(request.service, fontWeight = FontWeight.Bold)
                StatusBadge(request.status)
            }
            Text("Date: ${request.date}", fontSize = 12.sp, color = TextLightVal)
            Spacer(modifier = Modifier.height(8.dp))
            Text(request.description, fontSize = 14.sp)
            
            if (request.documentUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onViewDocument,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenVal),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Document", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminRequestCard(request: ServiceRequest, onUpdateStatus: (String, String) -> Unit, onPickFile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(request.requesterName, fontWeight = FontWeight.Bold)
                    Text("ID: ${request.studentId}", fontSize = 12.sp, color = TextLightVal)
                }
                StatusBadge(request.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Service: ${request.service}", fontWeight = FontWeight.Medium)
            Text(request.description, fontSize = 14.sp)
            
            if (request.status == "Pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onUpdateStatus(request.id, "Rejected") }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Reject") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onUpdateStatus(request.id, "Approved") }, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreenVal)) { Text("Approve") }
                }
            } else if (request.status == "Approved" && request.documentUrl == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Result Document")
                }
            } else if (request.documentUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreenVal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Document Sent to Student", fontSize = 12.sp, color = SuccessGreenVal, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlueVal.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text(user.name.take(1), fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text(user.email, fontSize = 12.sp, color = TextLightVal)
            }
            Spacer(modifier = Modifier.weight(1f))
            StatusBadge(user.role)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Admin", "Completed" -> SuccessGreenVal
        "Pending" -> AccentOrangeVal
        "Rejected" -> Color.Red
        else -> TextLightVal
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = TextLightVal)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement, isAdmin: Boolean, isJoined: Boolean, onJoin: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(announcement.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlueVal)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextLightVal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(announcement.date, fontSize = 12.sp, color = TextLightVal)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextLightVal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(announcement.location, fontSize = 12.sp, color = TextLightVal)
                    }
                }
                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(announcement.description, fontSize = 14.sp, color = TextDarkVal)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlueVal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${announcement.participants.size} Students Joined", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (!isAdmin) {
                    if (isJoined) {
                        Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(disabledContainerColor = SuccessGreenVal.copy(alpha = 0.2f))) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreenVal)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Joined", color = SuccessGreenVal)
                        }
                    } else {
                        Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVal)) {
                            Text("Join Event")
                        }
                    }
                }
            }

            if (isAdmin && announcement.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Joined Students:", fontSize = 11.sp, color = TextLightVal, fontWeight = FontWeight.SemiBold)
                Text(announcement.participants.joinToString(", "), fontSize = 12.sp, color = TextDarkVal.copy(alpha = 0.8f))
            }
        }
    }
}
