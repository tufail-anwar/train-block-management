# Architecture

```text
TMS / SMMS / TDMS / COA / CSV
             |
             v
     Integration Layer
             |
             v
      Spring Boot API
          /       \
         v         v
 PostgreSQL    Optimization Service
                    |
                    v
             Optimized Plan
                    |
                    v
              React Frontend
```

Backend: Java + Spring Boot.
Database: PostgreSQL.
Optimization: Python, with OR-Tools as a prototype option.
Frontend: React.
