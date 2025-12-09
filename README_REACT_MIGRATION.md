# React Migration Guide

This project has been migrated from a Flask template-based frontend to a React frontend with dark theme support.

## Project Structure

```
clarifai/
├── app/                    # Flask backend (unchanged)
│   ├── static/
│   │   └── react-build/    # React build output (generated)
│   └── ...
├── src/                     # React frontend source
│   ├── components/          # React components
│   ├── pages/               # Page components
│   ├── contexts/            # React contexts (Theme)
│   └── ...
├── package.json             # React dependencies
├── vite.config.js          # Vite build configuration
└── index.html              # React entry point
```

## Setup Instructions

### 1. Install React Dependencies

```bash
npm install
```

### 2. Build React Frontend

```bash
npm run build
```

This will build the React app and output it to `app/static/react-build/`.

### 3. Run Flask Backend

```bash
python run.py
```

The Flask backend will:
- Serve the React app for all routes (except API/auth routes)
- Handle API endpoints for authentication and data
- Serve static files from `app/static/`

## Development Mode

For development, you can run both React dev server and Flask backend:

### Terminal 1 - React Dev Server
```bash
npm run dev
```

### Terminal 2 - Flask Backend
```bash
python run.py
```

The React dev server runs on port 3000 with proxy to Flask on port 5000.

## Features

### Dark Theme Support

- Toggle button in navbar (🌙/☀️)
- Dark backgrounds for CFG and AST visualizations
- Light text/content for visibility
- Theme preference saved in localStorage

### React Components

- **Home**: Landing page with hero section and "How it works"
- **Model**: Main code editor page with AST, comments, and CFG
- **Dashboard**: View and manage code submissions
- **Login/Signup**: Authentication pages
- **Settings**: Account management

### API Endpoints

- `/api/check-auth`: Check authentication status
- `/api/dashboard`: Get dashboard data (submissions)
- `/generate-cfg`: Generate CFG with theme support
- `/ast-json`: Get AST JSON data
- `/get-submission/<id>`: Get submission details
- `/delete-submission/<id>`: Delete submission
- `/rename-submission/<id>`: Rename submission

## Migration Notes

- Flask backend remains unchanged for core functionality
- All routes now serve React app (except API/auth routes)
- Dark theme applied to CFG SVG generation
- AST visualization uses D3.js with theme-aware colors
- Monaco Editor theme switches based on user preference

## Troubleshooting

1. **React build not found**: Run `npm run build` first
2. **API routes not working**: Ensure Flask backend is running on port 5000
3. **Dark theme not applying**: Check browser localStorage for theme preference
4. **CFG/AST not dark**: Ensure theme is passed correctly in API calls

