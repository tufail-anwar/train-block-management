# System Design

## Roles
- Admin
- Department User
- Planner
- Approver

## Screens
1. Login
2. Dashboard
3. Maintenance Tasks
4. Block Requests
5. Train Schedule
6. Corridor/Asset Availability
7. Optimization Workspace
8. Weekly Planner
9. Monthly Planner
10. Conflict Center
11. What-if Analysis
12. Reports
13. Audit Logs

## Main flow
Department User -> maintenance task -> block request -> validation -> planner queue

Planner -> choose horizon -> run optimizer -> review conflicts -> compare alternatives -> edit/prepare plan

Approver -> review -> approve/reject

## UX principles
Simple, readable, role-based, explainable, desktop/tablet friendly, with human override and audit trail.
