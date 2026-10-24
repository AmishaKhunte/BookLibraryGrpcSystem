# React Setup Guide

This document explains how to set up and run the React UI for the BookLibraryGrpcSystem project.

## Prerequisites

Before running the frontend, make sure the following are installed:

- Node.js 18+
- npm 9+
- Git

Verify installation:

```bash
node -v
npm -v
```

## 1) Open the project root

```bash
cd BookLibraryGrpcSystem
```

## 2) Install the frontend dependencies

```bash
cd react-ui
npm install
```

## 3) Start the React development server

```bash
npm run dev -- --host 0.0.0.0
```

The app will start on:

```text
http://localhost:5173
```

## 4) Run the backend services before testing the UI

The React UI calls the LibraryService REST API, so the following services should already be running:

1. BookService (gRPC server)
2. LibraryService (REST + gRPC client)

Run them in separate terminals:

```bash
cd BookService
mvn spring-boot:run
```

```bash
cd LibraryService
mvn spring-boot:run
```

Then open the browser at:

```text
http://localhost:5173
```

## 5) UI features

The React page allows you to:

- List all books
- Fetch a book by ID
- Check book availability
- View JSON responses from the backend

## 6) API endpoints used by the UI

The frontend uses the following backend URLs:

```text
http://localhost:8080/api/library/books
http://localhost:8080/api/library/books/1
http://localhost:8080/api/library/books/1/availability
```

## 7) Production build

To build the React app for production:

```bash
cd react-ui
npm run build
```

To preview the production build:

```bash
npm run preview -- --host 0.0.0.0
```

## 8) Troubleshooting

### Port 5173 is already in use

Change the port in `react-ui/vite.config.js` or stop the conflicting process.

### UI shows backend errors

Make sure both Java services are running before opening the UI.

### Missing dependencies

Reinstall dependencies:

```bash
rm -rf node_modules
npm install
```

## 9) Project structure

```text
react-ui/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.jsx
│   └── styles.css
```

This front-end is a minimal testing UI for demonstrating the gRPC microservice concept in a browser.
