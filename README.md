# YumYum – Recipe Sharing Application

A web application where users can discover recipes, share their own recipes, and interact with the community through ratings and reviews.

A team project developed by three members using Java, Spring Boot, Thymeleaf, and MySQL.

---

## Features / 主な機能

### User Management

- User registration, login, and logout
- Session-based authentication
- Password hashing with BCrypt
- User profile management with bio and profile image uploads

### Recipe Management

- Create, view, edit, and delete recipes
- Ownership-based access control for editing and deleting recipes
- Discover recipes using area and ingredient filters
- Integration with TheMealDB API for external recipe discovery

### Ratings and Reviews

- Rate recipes from 1 to 5 stars
- Write and manage reviews
- One review per user for each recipe
- Prevent users from reviewing their own recipes
- Display average ratings and review counts

### Performance

- In-memory caching for recommended recipes to reduce repeated external API requests

---

## Tech Stack / 使用技術

**Backend:** Java, Spring Boot, Spring MVC, Spring Data JPA

**Frontend:** Thymeleaf, HTML, CSS

**Database:** MySQL

**External API:** TheMealDB API

**Tools:** Git, GitHub, IntelliJ IDEA

---

## My Contributions / 担当した部分

Focused primarily on backend development and application logic as part of a three-member team.

- Implemented user authentication, session management, and password hashing.
- Developed recipe CRUD functionality with ownership checks.
- Implemented user profile management and profile image uploads.
- Built the ratings and review system with validation and database queries.
- Integrated TheMealDB API for recipe discovery and filtering.
- Added in-memory caching for recommended recipes.
- Worked with teammates to integrate backend functionality with the frontend.

---

## What I Learned / 学んだこと

- Developing a server-side web application with Spring Boot and Thymeleaf
- Designing database relationships and implementing queries with JPA
- Managing authentication, sessions, and access control
- Integrating third-party APIs into a web application
- Implementing file uploads and application-level caching
- Collaborating with teammates and integrating frontend and backend features

---

## Running Locally / 起動方法

### Prerequisites

- Java 21
- MySQL
- Maven

### Database Setup

```sql
CREATE DATABASE yumyum;
```

### Backend Setup

```bash
./mvnw spring-boot:run
```

Configure your MySQL connection settings in `application.properties` before starting the application.
