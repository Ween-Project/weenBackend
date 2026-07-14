# User Flow

This document describes the primary user journeys supported by the Ween backend.

## Volunteer Journey

```mermaid
flowchart TD
    Start["Open platform"]
    Register["Register volunteer account"]
    Verify["Verify email"]
    Login["Log in"]
    Browse["Browse public events"]
    Detail["Open event detail"]
    RegisterEvent["Register for event"]
    Attend["Attend event"]
    QR["Use QR check-in"]
    Complete["Event completed"]
    Rewards["Receive coins, certificate, badge progress"]
    Social["Post impact update and follow users"]

    Start --> Register --> Verify --> Login --> Browse --> Detail --> RegisterEvent --> Attend --> QR --> Complete --> Rewards --> Social
```

## Organization Journey

```mermaid
flowchart TD
    OrgRegister["Register organization"]
    OrgLogin["Organization login"]
    Pending["Wait for verification if required"]
    Profile["Update organization profile"]
    Invite["Invite organizers"]
    CreateEvent["Create event as draft"]
    Publish["Publish event"]
    Manage["Track registrations and participants"]
    Start["Start event"]
    CheckIn["Verify check-ins"]
    Complete["Complete event"]
    Review["Review stats and participants"]

    OrgRegister --> OrgLogin --> Pending --> Profile --> Invite --> CreateEvent --> Publish --> Manage --> Start --> CheckIn --> Complete --> Review
```

## Admin Journey

```mermaid
flowchart TD
    AdminLogin["Admin login"]
    Dashboard["View platform statistics"]
    Users["Moderate users"]
    Orgs["Verify or reject organizations"]
    Events["Review and manage events"]
    Content["Moderate posts and comments"]
    Badges["Create and update badges"]
    Coins["Adjust user coins"]
    Audit["Review audit logs"]

    AdminLogin --> Dashboard
    Dashboard --> Users
    Dashboard --> Orgs
    Dashboard --> Events
    Dashboard --> Content
    Dashboard --> Badges
    Dashboard --> Coins
    Dashboard --> Audit
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as Ween API
    participant DB as Database

    U->>API: POST /api/v1/auth/register
    API->>DB: Create user and token records
    API-->>U: 201 Created with auth response and cookies
    U->>API: POST /api/v1/auth/login
    API->>DB: Validate account
    API-->>U: 200 OK with access and refresh tokens
    U->>API: Protected API request
    API-->>U: Protected resource
    U->>API: POST /api/v1/auth/refresh
    API-->>U: New access token
    U->>API: POST /api/v1/auth/logout
    API-->>U: Cookies cleared
```

## Event Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> PUBLISHED: publish
    PUBLISHED --> ONGOING: start
    ONGOING --> COMPLETED: complete
    PUBLISHED --> CANCELLED: cancel
    ONGOING --> CANCELLED: cancel
    COMPLETED --> [*]
    CANCELLED --> [*]
```

## Volunteer Event Registration Flow

```mermaid
sequenceDiagram
    participant V as Volunteer
    participant API as Ween API
    participant DB as Database
    participant O as Organizer

    V->>API: GET /api/v1/events
    API-->>V: Published events
    V->>API: POST /api/v1/events/{id}/register
    API->>DB: Create event registration
    API-->>V: Registration successful
    O->>API: GET /api/v1/events/{id}/participants
    API-->>O: Participant list
    V->>API: GET /api/v1/qr/generate
    API-->>V: QR token
    V->>API: POST /api/v1/participations/checkin-join
    API->>DB: Save participation status
    API-->>V: Check-in successful
```
