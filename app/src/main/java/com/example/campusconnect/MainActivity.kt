package com.example.campusconnect

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- UI Constants ---
val PrimaryBlueVal = Color(0xFF1A237E)
val SecondaryBlueVal = Color(0xFF3949AB)
val AccentOrangeVal = Color(0xFFFF9800)
val BackgroundGrayVal = Color(0xFFF5F7FA)
val SurfaceWhiteVal = Color(0xFFFFFFFF)
val TextDarkVal = Color(0xFF212121)
val TextLightVal = Color(0xFF757575)

// Permanently store data using DataStore
private val android.content.Context.dataStore by preferencesDataStore(name = "campus_connect_prefs")

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
    SPLASH, LOGIN, REGISTER, DASHBOARD, ADMIN_DASHBOARD, REQUEST_FORM, MY_REQUESTS, STUDENT_REQUESTS, ANNOUNCEMENTS, CREATE_ANNOUNCEMENT, USERS, LOGOUT
}

data class ServiceRequest(
    val id: String,
    val requesterName: String,
    val studentId: String,
    val service: String,
    val description: String,
    val status: String,
    val date: String,
    val price: String
)

data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val location: String,
    val description: String
)

@Composable
fun CampusConnectApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val auth = remember { FirebaseAuth.getInstance() }
    val requestsKey = remember { stringPreferencesKey("requests_list") }
    val announcementsKey = remember { stringPreferencesKey("announcements_list") }

    var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    
    // States for persistent data
    var currentUserData by remember { mutableStateOf<User?>(null) }
    var allUsersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var requestList by remember { mutableStateOf(listOf<ServiceRequest>()) }
    var announcementList by remember {
        mutableStateOf(listOf(
            Announcement(title = "Campus Foundation Day", date = "Oct 25, 2025", location = "Main Plaza", description = "Celebrating 50 years of excellence."),
            Announcement(title = "IT Seminar 2025", date = "Nov 12, 2025", location = "Audio Visual Room", description = "Trends in AI and Machine Learning."),
            Announcement(title = "Dorm Night", date = "Dec 05, 2025", location = "Student Center", description = "A night of music and social gathering.")
        ))
    }

    // Load data from permanent storage and Firebase on app launch
    LaunchedEffect(Unit) {
        try {
            // Check current user session
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val user = FirebaseManager.getUserData(firebaseUser.uid)
                currentUserData = user
            }

            val prefs = context.dataStore.data.first()
            
            prefs[requestsKey]?.let { json ->
                val type = object : TypeToken<List<ServiceRequest>>() {}.type
                requestList = gson.fromJson(json, type)
            }
            
            prefs[announcementsKey]?.let { json ->
                val type = object : TypeToken<List<Announcement>>() {}.type
                announcementList = gson.fromJson(json, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Functions to update and persist data
    fun saveRequests(newList: List<ServiceRequest>) {
        requestList = newList
        scope.launch { context.dataStore.edit { it[requestsKey] = gson.toJson(newList) } }
    }

    fun saveAnnouncements(newList: List<Announcement>) {
        announcementList = newList
        scope.launch { context.dataStore.edit { it[announcementsKey] = gson.toJson(newList) } }
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
            scope.launch {
                allUsersList = FirebaseManager.getAllUsers()
            }
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
                                                val user = FirebaseManager.getUserData(uid)
                                                currentUserData = user
                                                if (user != null) {
                                                    currentScreen = if (user.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD
                                                } else {
                                                    Toast.makeText(context, "User data not found in Firestore", Toast.LENGTH_SHORT).show()
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
                                                    FirebaseManager.createUserInFirestore(uid, name, email, role)
                                                    Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                                                    currentScreen = Screen.LOGIN
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        onNavigateToRequest = { currentScreen = Screen.REQUEST_FORM },
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                1 -> Screen.MY_REQUESTS
                                2 -> Screen.ANNOUNCEMENTS
                                else -> Screen.DASHBOARD
                            }
                        },
                        onLogoutClick = { currentScreen = Screen.LOGOUT }
                    )
                    Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                        user = currentUserData,
                        requestCount = requestList.size,
                        userCount = allUsersList.size,
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                1 -> Screen.STUDENT_REQUESTS
                                2 -> Screen.ANNOUNCEMENTS
                                3 -> Screen.USERS
                                else -> Screen.ADMIN_DASHBOARD
                            }
                        },
                        onLogoutClick = { currentScreen = Screen.LOGOUT }
                    )
                    Screen.REQUEST_FORM -> RequestFormScreen(
                        user = currentUserData,
                        onSubmitRequest = { name, id, service, desc ->
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())
                            val basePrice = 100
                            val autoPrice = (basePrice + (requestList.size * 50)).toString()
                            
                            val newRequest = ServiceRequest(
                                id = (requestList.size + 1001).toString(),
                                requesterName = name,
                                studentId = id,
                                service = service,
                                description = desc,
                                status = "Pending",
                                date = currentDate,
                                price = autoPrice
                            )
                            saveRequests(requestList + newRequest)
                            Toast.makeText(context, "Request Submitted Successfully", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.MY_REQUESTS
                        },
                        onBack = { currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD }
                    )
                    Screen.MY_REQUESTS -> MyRequestsScreen(
                        requestList = requestList.filter { it.requesterName == (currentUserData?.name ?: "") },
                        title = "My Requests",
                        isAdmin = currentUserData?.role == "Admin",
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                0 -> if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD
                                2 -> Screen.ANNOUNCEMENTS
                                else -> Screen.MY_REQUESTS
                            }
                        },
                        onAcceptRequest = { requestId ->
                            saveRequests(requestList.map { 
                                if (it.id == requestId) it.copy(status = "Approved") else it
                            })
                        },
                        onDeleteRequest = { requestId ->
                            saveRequests(requestList.filter { it.id != requestId })
                        },
                        onNavigateToOther = { if (currentUserData?.role == "Admin") currentScreen = Screen.STUDENT_REQUESTS },
                        buttonText = if (currentUserData?.role == "Admin") "Go to Student Requests" else "",
                        onLogoutClick = { currentScreen = Screen.LOGOUT }
                    )
                    Screen.STUDENT_REQUESTS -> MyRequestsScreen(
                        requestList = requestList,
                        title = "Student Requests",
                        isAdmin = true,
                        onTabClick = { tabIndex ->
                            currentScreen = when(tabIndex) {
                                0 -> Screen.ADMIN_DASHBOARD
                                1 -> Screen.MY_REQUESTS
                                2 -> Screen.ANNOUNCEMENTS
                                3 -> Screen.USERS
                                else -> Screen.STUDENT_REQUESTS
                            }
                        },
                        onAcceptRequest = { requestId ->
                            saveRequests(requestList.map { 
                                if (it.id == requestId) it.copy(status = "Approved") else it
                            })
                        },
                        onDeleteRequest = { requestId ->
                            saveRequests(requestList.filter { it.id != requestId })
                        },
                        onNavigateToOther = { currentScreen = Screen.MY_REQUESTS },
                        buttonText = "Go to My Requests",
                        onLogoutClick = { currentScreen = Screen.LOGOUT }
                    )
                    Screen.ANNOUNCEMENTS -> AnnouncementsScreen(
                        announcements = announcementList,
                        onBack = { currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD },
                        onAddClick = { currentScreen = Screen.CREATE_ANNOUNCEMENT },
                        onDeleteAnnouncement = { announcement ->
                            saveAnnouncements(announcementList.filter { it.id != announcement.id })
                        },
                        isAdmin = currentUserData?.role == "Admin"
                    )
                    Screen.CREATE_ANNOUNCEMENT -> CreateAnnouncementScreen(
                        onAddAnnouncement = { title, date, location, description ->
                            val newAnnouncement = Announcement(title = title, date = date, location = location, description = description)
                            saveAnnouncements(announcementList + newAnnouncement)
                            Toast.makeText(context, "Announcement Posted Successfully", Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.ANNOUNCEMENTS
                        },
                        onBack = { currentScreen = Screen.ANNOUNCEMENTS }
                    )
                    Screen.USERS -> UsersScreen(
                        users = allUsersList,
                        onBack = { currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD }
                    )
                    Screen.LOGOUT -> LogoutScreen(
                        onConfirmLogout = { 
                            auth.signOut()
                            currentUserData = null
                            currentScreen = Screen.LOGIN 
                        },
                        onCancel = { currentScreen = if (currentUserData?.role == "Admin") Screen.ADMIN_DASHBOARD else Screen.DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryBlueVal, SecondaryBlueVal))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoComponent(size = 180.dp, borderEnabled = false)
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "CAMPUS CONNECT",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = alpha),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp)),
                color = AccentOrangeVal,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun LogoComponent(size: Dp, borderEnabled: Boolean = true) {
    Surface(
        modifier = Modifier
            .size(size)
            .shadow(12.dp, CircleShape),
        shape = CircleShape,
        color = Color.White,
        border = if (borderEnabled) BorderStroke(3.dp, PrimaryBlueVal) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(size * 0.55f),
                tint = PrimaryBlueVal
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit, 
    onNavigateToRegister: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f).background(Brush.verticalGradient(listOf(PrimaryBlueVal, SecondaryBlueVal))))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoComponent(size = 140.dp)
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
                    Text("Login to your campus account", fontSize = 14.sp, color = TextLightVal)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ModernTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                onLoginSuccess(email, password)
                            } else {
                                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVal)
                    ) {
                        Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Don't have an account? Register here",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlueVal,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@Composable
fun RoleSelector(selectedRole: String, onRoleSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedRole == "Student",
            onClick = { onRoleSelected("Student") },
            label = { Text("Student") },
            leadingIcon = { if (selectedRole == "Student") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        FilterChip(
            selected = selectedRole == "Admin",
            onClick = { onRoleSelected("Admin") },
            label = { Text("Admin") },
            leadingIcon = { if (selectedRole == "Admin") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: (String, String, String, String) -> Unit, onBackToLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf("Student") }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f).background(Brush.horizontalGradient(listOf(SecondaryBlueVal, PrimaryBlueVal))))
        
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(40.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RoleSelector(selectedRole = selectedRole, onRoleSelected = { selectedRole = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernTextField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Default.Person)
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernTextField(value = password, onValueChange = { password = it }, label = "Create Password", icon = Icons.Default.Lock, isPassword = true)
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Button(
                        onClick = { 
                            if(name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                                onRegisterSuccess(name, email, password, selectedRole) 
                            } else {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("REGISTER", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onBackToLogin) {
                Text("Already have an account? Login", color = PrimaryBlueVal)
            }
        }
    }
}

@Composable
fun DashboardScreen(user: User?, onNavigateToRequest: () -> Unit, onTabClick: (Int) -> Unit, onLogoutClick: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val services = listOf(
        Service("Document Request", "Official university transcripts and certifications.", Icons.Default.Description),
        Service("Counseling", "Confidential support for mental health and well-being.", Icons.Default.Person),
        Service("Dorm Maintenance", "Report issues in student housing facilities.", Icons.Default.HomeWork),
        Service("IT Support", "Assistance with network, accounts, and hardware.", Icons.Default.Computer)
    )
    
    val filteredServices = services.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(title = "Campus Connect", welcomeText = "Hi, ${user?.name ?: "Student"}", onLogout = onLogoutClick)
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TabRow(
                selectedTabIndex = 0,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlueVal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[0]),
                        color = PrimaryBlueVal
                    )
                },
                divider = {}
            ) {
                listOf("Services", "My Requests", "Announcements").forEachIndexed { index, title ->
                    Tab(
                        selected = index == 0,
                        onClick = { if(index > 0) onTabClick(index) },
                        text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
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

@Composable
fun AdminDashboardScreen(user: User?, requestCount: Int, userCount: Int, onTabClick: (Int) -> Unit, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundGrayVal)) {
        DashboardHeader(title = "Admin Panel", welcomeText = "Admin: ${user?.name}", onLogout = onLogoutClick, isAdmin = true)
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TabRow(selectedTabIndex = 0, containerColor = Color.Transparent, contentColor = PrimaryBlueVal, divider = {}) {
                listOf("Dashboard", "Requests", "Announcements", "Users").forEachIndexed { index, title ->
                    Tab(selected = index == 0, onClick = { if(index > 0) onTabClick(index) }, text = { Text(title, fontSize = 12.sp) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCardModern(title = "Requests", count = requestCount.toString(), icon = Icons.AutoMirrored.Filled.Assignment, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                StatCardModern(title = "Users", count = userCount.toString(), icon = Icons.Default.Group, color = PrimaryBlueVal, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Quick Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { AdminActionItem(title = "Review Student Submissions", icon = Icons.Default.PendingActions, onClick = { onTabClick(1) }) }
                item { AdminActionItem(title = "Broadcast Announcement", icon = Icons.Default.Campaign, onClick = { onTabClick(2) }) }
                item { AdminActionItem(title = "User Management", icon = Icons.Default.ManageAccounts, onClick = { onTabClick(3) }) }
            }
        }
    }
}

@Composable
fun DashboardHeader(title: String, welcomeText: String, onLogout: () -> Unit, isAdmin: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isAdmin) Color(0xFFB71C1C) else PrimaryBlueVal,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoComponent(size = 48.dp, borderEnabled = false)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(welcomeText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
            }
        }
    }
}

@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhiteVal,
        shadowElevation = 2.dp
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Search services...", color = TextLightVal) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLightVal) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

@Composable
fun ServiceCardModern(service: Service, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(PrimaryBlueVal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(service.icon, contentDescription = null, tint = PrimaryBlueVal, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDarkVal)
                Text(service.description, fontSize = 12.sp, color = TextLightVal, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun StatCardModern(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(count, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
            Text(title, fontSize = 14.sp, color = TextLightVal)
        }
    }
}

@Composable
fun AdminActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhiteVal,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextLightVal)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = TextDarkVal)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun RequestFormScreen(user: User?, onSubmitRequest: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var requesterName by rememberSaveable { mutableStateOf(user?.name ?: "") }
    var studentId by rememberSaveable { mutableStateOf("") }
    var serviceType by rememberSaveable { mutableStateOf("General") }
    var description by rememberSaveable { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(title = "New Request", welcomeText = "Fill in the details below", onLogout = onBack)
        
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModernTextField(value = requesterName, onValueChange = { requesterName = it }, label = "Your Name", icon = Icons.Default.Person)
            ModernTextField(value = studentId, onValueChange = { studentId = it }, label = "Student ID", icon = Icons.Default.Badge)
            ModernTextField(value = serviceType, onValueChange = { serviceType = it }, label = "Service Category", icon = Icons.Default.Category)
            
            Column {
                Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextLightVal, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlueVal)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { if(requesterName.isNotEmpty()) onSubmitRequest(requesterName, studentId, serviceType, description) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SUBMIT REQUEST", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MyRequestsScreen(requestList: List<ServiceRequest>, title: String, isAdmin: Boolean, onTabClick: (Int) -> Unit, onAcceptRequest: (String) -> Unit, onDeleteRequest: (String) -> Unit, onNavigateToOther: () -> Unit, buttonText: String, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(title = title, welcomeText = "Management Dashboard", onLogout = onLogoutClick)
        
        Column(modifier = Modifier.padding(20.dp)) {
            TabRow(selectedTabIndex = 1, containerColor = Color.Transparent, contentColor = PrimaryBlueVal, divider = {}) {
                listOf("Dashboard", "Requests", "Announcements").forEachIndexed { index, t ->
                    Tab(selected = (index == 1), onClick = { onTabClick(index) }, text = { Text(t, fontSize = 12.sp) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (buttonText.isNotEmpty()) {
                Button(
                    onClick = onNavigateToOther,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlueVal)
                ) {
                    Text(buttonText)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(requestList) { request -> 
                    RequestItemCardModern(request = request, isAdmin = isAdmin, onAccept = { onAcceptRequest(request.id) }, onDelete = { onDeleteRequest(request.id) }) 
                }
            }
        }
    }
}

@Composable
fun RequestItemCardModern(request: ServiceRequest, isAdmin: Boolean, onAccept: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlueVal.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(request.service.take(1).uppercase(), color = PrimaryBlueVal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.service, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDarkVal)
                    Text("ID: ${request.studentId} • ${request.date}", fontSize = 12.sp, color = TextLightVal)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(request.description, fontSize = 14.sp, color = TextDarkVal.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = request.status)
                Spacer(modifier = Modifier.weight(1f))
                if (isAdmin && request.status == "Pending") {
                    TextButton(onClick = onAccept) {
                        Text("Approve Now", color = PrimaryBlueVal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when(status) {
        "Approved" -> Color(0xFF4CAF50)
        "Pending" -> AccentOrangeVal
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnnouncementsScreen(announcements: List<Announcement>, onBack: () -> Unit, onAddClick: () -> Unit, onDeleteAnnouncement: (Announcement) -> Unit, isAdmin: Boolean) {
    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = onAddClick, containerColor = AccentOrangeVal) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                }
            }
        },
        containerColor = BackgroundGrayVal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DashboardHeader(title = "Announcements", welcomeText = "Campus Updates", onLogout = onBack, isAdmin = isAdmin)
            
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(announcements) { ann ->
                    AnnouncementCard(ann = ann, onDelete = { onDeleteAnnouncement(ann) })
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(ann: Announcement, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = AccentOrangeVal)
                Spacer(modifier = Modifier.width(12.dp))
                Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkVal, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { 
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Announcement", tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextLightVal)
                Text(" ${ann.date} • ", fontSize = 12.sp, color = TextLightVal)
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextLightVal)
                Text(" ${ann.location}", fontSize = 12.sp, color = TextLightVal)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(ann.description, fontSize = 14.sp, color = TextDarkVal.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun CreateAnnouncementScreen(onAddAnnouncement: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(title = "New Announcement", welcomeText = "Broadcast to Campus", onLogout = onBack, isAdmin = true)
        
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernTextField(value = title, onValueChange = { title = it }, label = "Announcement Title", icon = Icons.Default.Title)
            ModernTextField(value = date, onValueChange = { date = it }, label = "Event Date", icon = Icons.Default.CalendarToday)
            ModernTextField(value = location, onValueChange = { location = it }, label = "Location", icon = Icons.Default.Room)
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Announcement Details") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(16.dp)
            )
            
            Button(
                onClick = { if(title.isNotEmpty()) onAddAnnouncement(title, date, location, description) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueVal)
            ) {
                Text("POST ANNOUNCEMENT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UsersScreen(users: List<User>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DashboardHeader(title = "User Directory", welcomeText = "Manage Campus Accounts", onLogout = onBack, isAdmin = true)
        
        LazyColumn(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                UserCard(user = user)
            }
        }
    }
}

@Composable
fun UserCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = BackgroundGrayVal) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextLightVal)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold, color = TextDarkVal)
                Text(user.email, fontSize = 12.sp, color = TextLightVal)
            }
            Spacer(modifier = Modifier.weight(1f))
            StatusBadge(status = user.role)
        }
    }
}

@Composable
fun LogoutScreen(onConfirmLogout: () -> Unit, onCancel: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrimaryBlueVal, SecondaryBlueVal))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            LogoComponent(size = 120.dp)
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhiteVal)
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Ready to Leave?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkVal)
                    Text("We hope to see you back soon!", color = TextLightVal, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Button(
                        onClick = onConfirmLogout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("LOGOUT NOW", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryBlueVal)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, color = PrimaryBlueVal)
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlueVal) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlueVal,
            focusedLabelColor = PrimaryBlueVal,
            cursorColor = PrimaryBlueVal
        )
    )
}

data class Service(val title: String, val description: String, val icon: ImageVector)
