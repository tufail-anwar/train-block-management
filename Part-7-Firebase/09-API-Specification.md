# API Specification

POST /api/auth/login

POST /api/maintenance
GET /api/maintenance
GET /api/maintenance/{id}
PUT /api/maintenance/{id}

POST /api/block-requests
GET /api/block-requests

GET /api/trains
GET /api/train-schedules
GET /api/corridors
GET /api/corridors/{id}/availability

POST /api/optimization/run
GET /api/optimization/runs/{id}
GET /api/optimization/runs/{id}/results
POST /api/optimization/compare

GET /api/conflicts
GET /api/plans/weekly
GET /api/plans/monthly
POST /api/plans/{id}/approve
POST /api/plans/{id}/reject

GET /api/analytics/summary
GET /api/analytics/before-after
