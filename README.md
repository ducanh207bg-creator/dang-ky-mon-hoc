# Đăng ký môn học (Course Registration)

A demo app where students log in, view courses, and register or drop them. It has a web page, a Swing desktop app and a text menu. It is written in Java and stores data in SQLite.

**Online demo:** https://ducanh207bg-creator.github.io/dang-ky-mon-hoc/

The online demo (the `docs` folder) is a static copy of the web page. It has no password and no Java server. Its data stays in your own browser, so people don't share class sizes. To run the real app, follow the steps below.

## Group members

The members are also the sample accounts.

| Student ID | Name |
|---|---|
| SV01 | Trần Văn Đức Anh |
| SV02 | Nguyễn Văn Long |
| SV03 | Nguyễn Tất Cương |
| SV04 | Nguyễn Quang Minh |

All four accounts share one demo password. It is in a comment in [`src/data/setup.sql`](src/data/setup.sql).

## Requirements

- Windows with PowerShell (the commands below are written for it)
- JDK 17 or newer. The project has been tested with JDK 27 only.
- Internet access on the first build only if `src/lib/sqlite.jar` or `src/lib/gson.jar` is missing

Check your version:

```powershell
javac -version
```

If `javac` is not found, or the version is older than 17, point the current PowerShell window at your JDK. Change the path to match your computer.

```powershell
$env:PATH = "$env:USERPROFILE\.jdks\openjdk-27\bin;$env:PATH"
```

Do this in every new PowerShell window. It makes both `javac` and `java` come from that JDK.

## Quick start

On Windows, double-click **`run.bat`** in the project folder. It:

1. builds the project
2. starts the server in a minimized window named **Course server**
3. opens the Swing app and the web page

To stop the server, close the **Course server** window. If the server is already running, `run.bat` skips the build and just opens the app. So after changing code, close that window first.

`run.bat` looks for a JDK in `%USERPROFILE%\.jdks`, then `JAVA_HOME`, then `PATH`. If it finds more than one in `.jdks`, it takes the last by folder name, which may not be the newest.

## How to run manually

Each block starts from the project folder (`test1`, the folder with this file).

### 1. Build

Build once, and again after changing any Java file.

```powershell
.\src\build.bat
```

On Linux, macOS or Git Bash, `sh src/build.sh` does the same job.

### 2. Start the server

The web page and the Swing app both need it. Leave this window open.

```powershell
cd src; java "-Djdk.net.unixdomain.tmpdir=$($PWD.Path)" --enable-native-access=ALL-UNNAMED -cp "out;lib/*" main.WebServer
```

It is ready when it prints `Mở http://127.0.0.1:8000`. Press `Ctrl+C` to stop it.

The `-D` option avoids a startup error that happens on some Windows computers (see [Troubleshooting](#troubleshooting)).

### 3. Use the web page

Open <http://127.0.0.1:8000> and log in.

### 4. Or use the Swing app

Open a second PowerShell window in the project folder, set up `PATH` again if you needed to, then run:

```powershell
cd src; java -cp "out;lib/*" main.SwingApp
```

Click a course in the table, then click **Đăng ký** (register) or **Hủy đăng ký** (drop).

### Text menu (optional)

The text menu does not need the server and has no login. It uses its own sample data in memory, so nothing is saved, and its data differs from the database (for example, IT103 has 2 credits here and 3 in the database).

```powershell
cd src; java "-Dstdout.encoding=UTF-8" -cp "out;lib/*" main.Main
```

If Vietnamese text looks wrong, run `chcp 65001` first. `-Dstdout.encoding` needs a recent JDK.

## Registration rules

A registration is checked for:

- free places in the course
- the student's credit limit (6 for the sample accounts)
- a clash with the time of another registered course

## Tests

Build first. The login tests start their own server on port 8765 with a temporary database, so your data is not changed.

```powershell
cd src; java "-Djdk.net.unixdomain.tmpdir=$($PWD.Path)" --enable-native-access=ALL-UNNAMED -cp "out;lib/*" tests/LoginTest.java
```

The last line should be `ALL PASSED`. The tests cover login and the server's access checks. They do not test the web page, the Swing windows or the registration rules in full.

## Data

- The database is `src/data/registration.db`. The server creates it on first start.
- On every start, the server runs `src/data/setup.sql`. This adds missing sample data and updates the four names. It keeps existing registrations, courses and passwords.
- To reset everything, stop the server and delete `src/data/registration.db`. **This removes all registrations.**

## Project layout

```
test1/
  README.md
  run.bat                    one-click build and run (Windows)
  src/
    build.bat, build.sh      build scripts
    data/setup.sql           tables and sample data
    lib/                     SQLite and Gson libraries
    web/index.html           web page
    src/main/                WebServer, SwingApp, LoginFrame, Theme, Main (text menu)
    src/model/               User, Student, Course, Schedule
    src/repository/          SqlRepository (database access)
    src/service/             login and registration rules
    tests/LoginTest.java     login tests
    docs/                    design notes and slides
    out/                     build output
  .claude/                   launch files for Claude Code
```

`src/Main.java` is IntelliJ's starter "Hello" file and is not part of the app.

## Server API

| Method and path | Needs login | Purpose |
|---|---|---|
| `POST /api/login` | No | Send `{"student": "...", "password": "..."}` and receive a token |
| `POST /api/logout` | No | End the session for the token sent |
| `GET /api/state` | Yes | Student details and courses (JSON). Optional: `q`, `registered=true` |
| `GET /api/text` | Yes | The same course list as plain text |
| `POST /api/change` | Yes | Send `{"course": "...", "action": "register"}` or `"drop"` |

Requests that need login send the header `Authorization: Bearer <token>`. The server uses the token to decide which student is making the request.

## Troubleshooting

**`javac` is not recognized, or there is a class version error.** Your terminal is using no JDK or an old Java. Set `PATH` as shown in [Requirements](#requirements).

**`Unable to establish loopback connection`.** Java could not connect through a temporary socket file, probably because of the temp folder path. The `-Djdk.net.unixdomain.tmpdir=...` option in the commands above puts that file in the `src` folder instead, so make sure you included it. If your folder path is very long, point the option at a shorter folder.

**`Address already in use`.** Another server is using port 8000. Stop it first. The Swing app always connects to port 8000.

**Swing says it cannot connect (`Không kết nối được máy chủ`).** Start the server and keep its window open.

**You are logged out after restarting the server.** This is expected. Log in again.

**`run-web.bat`.** `.claude\run-web.bat` is a shortcut that starts the server with the fix above. It expects the JDK at `%USERPROFILE%\.jdks\openjdk-27`, so edit that path on another computer.

**IntelliJ shows package errors (not tested).** The code is in `src/src`. Mark that folder as Sources Root, add the two `.jar` files in `src/lib` as libraries, and set the run configuration's working directory to `src`.

## Demo limits

This login is only for a local demo. The password is shared and written in the setup file, there is no limit on wrong attempts, you cannot change a password, and the server does not use HTTPS. Sessions last until logout or a server restart.
