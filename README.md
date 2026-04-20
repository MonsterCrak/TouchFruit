# TouchFruit - Sistema de Comandas Rápidas

Aplicación Android de gestión de pedidos tipo comanda rápida para restaurantes y juguerías.

## Roles

- **Emisor**: Crea pedidos selecting mesa, categoría y productos
- **Receptor**: Recibe y gestiona los pedidos de cocina

## Características

- Login con código de acceso (E=Emisor, R=Receptor)
- Creación de comandas con selección de mesa
- Flujo de pedido: Nuevo → En preparación → Listo
- Historial de sesiones por mesa
- Reportes: Diario, Semanal, Mensual
- Base de datos local con Room
- UI con Jetpack Compose y Material 3

## Arquitectura

```
├── data/
│   ├── local/          # Room database
│   ├── model/          # Domain models
│   └── repository/     # Single source of truth
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Screen composables
│   └── theme/          # Design tokens
└── viewmodel/          # ViewModels
```

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room Database
- StateFlow / Coroutines
- MVVM Architecture
