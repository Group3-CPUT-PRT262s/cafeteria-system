# Repository Module Overview

## Hey Team

Please take note of the following for this section of the project.

---

## What this module is

This repository module handles our database queries (for now).

Since we’re using **Spring Boot**, we don’t manually write most SQL queries. Instead, we define interfaces, and Spring automatically generates the standard queries for us.

This means we get methods like:

- `findAll()`
- `findById()`
- `save()`
- `delete()`

…without writing any SQL.

These are built into Spring Data JPA, so for now we’re just making sure the base application works correctly with the modules we’re building.

It’s worth understanding this properly when you get time, since it’s doing a lot of work behind the scenes.

---

## How Spring generates queries

This is the important part.

Spring can read method names and automatically generate SQL queries based on them.

### Examples:

| Method Name                                      | Generated SQL                                                      |
|--------------------------------------------------|--------------------------------------------------------------------|
| `findByIsActiveTrue()`                           | `SELECT * FROM menu_items WHERE is_active = true`                  |
| `findByUsername(String username)`                | `SELECT * FROM users WHERE username = ?`                           |
| `findByUsernameOrderByCreatedAtDesc()`           | `SELECT * FROM orders WHERE username = ? ORDER BY created_at DESC` |
| `findByIsActiveTrueAndCategory(String category)` | `SELECT * FROM menu_items WHERE is_active = true AND category = ?` |

---

## Why this works

We don’t manually write SQL for these cases.

Spring interprets the method names and builds the queries automatically (YAY!).

If we need more complex logic later, we can use:

```java
@Query