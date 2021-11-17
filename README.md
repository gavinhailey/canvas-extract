# Canvas Extract
Created by Gavin Hailey

### Status
All functions work properly as tested.

### Usage
- Build and assemble distributable by running `./gradlew build` and `./gradlew installDist` (Note: you made need to `chmod +x gradlew`)
- Unzip the dist from wherever you want to use it  
- Add **CANVAS_CALENDAR** and **ASSIGNMENTS_CALENDAR** to your environment variables, you must do this for cron as well.  
  This should be something like `CANVAS_CALENDAR = 1234567abcdefg@group.calendar.google.com`  
  You can get this from calendar > settings and sharing > calendar id
