# EventSphere – Smart Event Management System

## Overview

EventSphere is a full-stack web application designed to simplify and automate event management for universities, organizations, businesses, and communities. The platform enables organizers to create and manage events, participants to register online, and administrators to monitor system activities through dashboards and reports.

The system supports various event types including:

* University Events
* Conferences
* Workshops
* Seminars
* Meetings
* Weddings
* Social Gatherings

A key feature of EventSphere is the use of QR Code technology for participant validation and attendance tracking.

---

# Project Objectives

The objectives of EventSphere are:

* Simplify event management through digital automation.
* Allow participants to register for events online.
* Generate digital tickets and receipts automatically.
* Validate participants using QR codes.
* Track attendance in real time.
* Provide dashboards and reports for administrators and organizers.
* Improve communication between organizers and participants.
* Ensure secure authentication and authorization.

---

# Technology Stack

## Backend

* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* JWT Authentication
* Maven

## Frontend

* Vue.js 3
* Vue Router
* Axios
* Pinia
* Tailwind CSS

## Database

* MySQL

## API Testing

* Postman

---

# User Roles

## 1. Admin

The administrator manages the entire platform.

### Features

* Login/Logout
* Manage Users
* Approve Organizers
* Manage Categories
* View All Events
* Delete Events
* Generate Reports
* Attendance Analytics
* QR Validation Monitoring

---

## 2. Organizer

Organizers create and manage events.

### Features

* Register/Login
* Create Events
* Edit Events
* Delete Events
* Upload Event Banners
* Set Venue and Schedule
* View Participants
* Generate Attendance Reports
* Scan QR Codes
* Send Notifications

---

## 3. Participant

Participants attend events.

### Features

* Register/Login
* Browse Events
* Search Events
* Register for Events
* Download Receipts
* View QR Tickets
* Receive Notifications
* View Registration History

---

# Core Modules

## Authentication Module

### Features

* User Registration
* User Login
* JWT Authentication
* Password Encryption
* Forgot Password
* Role-Based Authorization

---

## Event Management Module

### Features

* Create Event
* Update Event
* Delete Event
* View Events
* Event Categories
* Event Scheduling
* Venue Management
* Banner Upload

### Event Information

* Event ID
* Title
* Description
* Category
* Venue
* Date
* Time
* Maximum Participants
* Organizer Information
* Banner Image
* Ticket Price

---

## Participant Registration Module

### Features

* Event Registration
* Unique Registration Number
* Capacity Control
* Attendance Status Tracking
* Registration Confirmation

---

## QR Code Ticket & Receipt Module

### Features

* QR Code Generation
* Ticket Generation
* PDF Receipt Download
* Secure Validation

### Receipt Information

* Event Name
* Participant Name
* Registration Number
* Event Date
* Venue
* Organizer Information
* QR Code
* Ticket ID
* Payment Status
* Timestamp

---

## Attendance Management Module

### Features

* QR Scanning
* Attendance Recording
* Present/Absent Tracking
* Attendance Reports
* Real-Time Dashboard

---

## Notification Module

### Features

* Registration Confirmation
* Event Reminders
* Event Updates
* Cancellation Alerts

Optional:

* Email Notifications
* SMS Notifications

---

## Dashboard & Analytics Module

### Admin Dashboard

* Total Users
* Total Events
* Total Participants
* Attendance Statistics
* Event Analytics

### Organizer Dashboard

* My Events
* Registered Participants
* Attendance Count
* QR Scan Statistics

---

# QR Code Validation Workflow

## Registration Process

1. Participant registers for an event.
2. System generates a unique registration number.
3. QR code is created automatically.
4. Digital receipt is generated.
5. Ticket information is stored in the database.
6. Participant downloads ticket and receipt.

## Attendance Verification Process

1. Organizer scans participant QR code.
2. System verifies:

   * QR code exists.
   * Participant belongs to the event.
   * Ticket has not been used.
   * Event date is valid.
3. Attendance is recorded automatically.
4. Duplicate entries are prevented.

---

# Database Structure

## Users Table

| Field      | Type      |
| ---------- | --------- |
| id         | BIGINT    |
| fullname   | VARCHAR   |
| email      | VARCHAR   |
| password   | VARCHAR   |
| role       | VARCHAR   |
| phone      | VARCHAR   |
| created_at | TIMESTAMP |

---

## Events Table

| Field            | Type    |
| ---------------- | ------- |
| id               | BIGINT  |
| title            | VARCHAR |
| description      | TEXT    |
| category_id      | BIGINT  |
| venue            | VARCHAR |
| date             | DATE    |
| time             | TIME    |
| organizer_id     | BIGINT  |
| max_participants | INTEGER |
| banner_image     | VARCHAR |

---

## Categories Table

| Field | Type    |
| ----- | ------- |
| id    | BIGINT  |
| name  | VARCHAR |

---

## Registrations Table

| Field               | Type      |
| ------------------- | --------- |
| id                  | BIGINT    |
| user_id             | BIGINT    |
| event_id            | BIGINT    |
| registration_number | VARCHAR   |
| qr_code             | TEXT      |
| attendance_status   | VARCHAR   |
| payment_status      | VARCHAR   |
| created_at          | TIMESTAMP |

---

## Attendance Table

| Field           | Type      |
| --------------- | --------- |
| id              | BIGINT    |
| registration_id | BIGINT    |
| scanned_at      | TIMESTAMP |
| verified_by     | BIGINT    |

---

## Notifications Table

| Field      | Type      |
| ---------- | --------- |
| id         | BIGINT    |
| title      | VARCHAR   |
| message    | TEXT      |
| user_id    | BIGINT    |
| created_at | TIMESTAMP |

---

# REST API Endpoints

## Authentication APIs

```http
POST /api/auth/register
POST /api/auth/login
```

## Event APIs

```http
GET /api/events
POST /api/events
PUT /api/events/{id}
DELETE /api/events/{id}
```

## Registration APIs

```http
POST /api/register-event
GET /api/my-registrations
```

## QR APIs

```http
POST /api/scan-qr
GET /api/ticket/{id}
```

## Dashboard APIs

```http
GET /api/admin/dashboard
GET /api/organizer/dashboard
```

---

# Frontend Pages

## Public Pages

* Home Page
* Event Listings
* Event Details
* Login
* Register

## Participant Pages

* Dashboard
* My Registrations
* Download Receipt
* QR Ticket
* Send the ticket for the particular participent via Email or SmS 

## Organizer Pages

* Create Event
* Manage Events
* Participants
* Attendance
* QR Scanner

## Admin Pages

* Dashboard
* User Management
* Event Management
* Reports
* Analytics
* Real SMS Notifications and Real Email delivary
---

# Security Features

* JWT Authentication
* Spring Security
* Password Encryption with BCrypt
* Role-Based Access Control
* Input Validation
* SQL Injection Prevention
* CORS Configuration

---

# Optional Advanced Features

* Email Verification
* Online Payment Integration
* Event Live Streaming
* PDF Report Export
* Webcam QR Scanner
* Calendar Integration


---

# Future Enhancements

* Mobile Application
* AI-Based Event Recommendations
* Multi-Language Support
* Event Rating System
* Cert Generation
* Social Media Sharing

---

# Installation

## Clone Repository

```bash
git clone https://github.com/abrahman90/eventsphere.git
```
##Sever port 
*for backend port  8080
*for fontend port 5173
you can confingure this by navigating to backend->Eventsphere->src->main->resources->Application.propeties
for the fontend confingure this by navigating fontend->src->api->index.js
## Backend Setup

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

## Database Setup

1. Install MySQL.
2. Create database:

```sql
CREATE DATABASE events;
```

3. Update application.properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/events
spring.datasource.username=root
spring.datasource.password=yourpassword
```

---

# Contributors

EventSphere 

---

# License

This project is developed for academic and educational purposes.
