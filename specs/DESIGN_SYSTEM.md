# Design System - Budget Calendar

## Overview

This document defines the visual design language, component library, and UI/UX specifications for the Budget Calendar Kotlin Multiplatform application. The design system ensures consistency across Android (Jetpack Compose) and iOS (SwiftUI) platforms while embracing the calendar-first approach to personal finance management.

---

## 1. Design Principles

### 1.1 Core Philosophy

The Budget Calendar visual language is built on the foundational principle that **time is the primary axis for understanding money**. Every UI element reinforces the relationship between temporal events and financial outcomes. Users should be able to glance at any day and immediately understand their financial position for that period.

### 1.2 Design Tenets

| Tenet | Description | Implementation |
|-------|-------------|----------------|
| **Temporal Clarity** | Time-based information is always primary and immediately visible | Calendar grid takes precedence, dates display financial summaries inline |
| **Financial Visibility** | Money in/money out is instantly recognizable | Color-coded amounts, clear positive/negative indicators |
| **Safe-to-Spend Awareness** | Users should always know their disposable funds | Prominent "Safe to Spend" display at all times |
| **Transaction State Clarity** | Pending, Overdue, and Confirmed states are visually distinct | Three-tier status system with unique visual treatments |
| **High Contrast Selection** | Active/selected elements demand attention | Dark-mode styling on selected day cells |

### 1.3 Accessibility Commitment

The design system adheres to WCAG 2.1 AA compliance standards:

- Minimum 4.5:1 contrast ratio for body text
- Minimum 3:1 contrast ratio for large text and UI components
- All interactive elements support keyboard navigation
- Screen reader compatibility for all components
- Touch targets minimum 44x44dp on Android, 44x44pt on iOS
- Support for dynamic type scaling (iOS) and font scaling (Android)

---

## 2. Color System

### 2.1 Design Token Variables

The design system uses CSS custom properties (variables) defined in the .pen file:

| Token | Hex Code | Usage |
|-------|----------|-------|
| `--bg-dark` | #000000 | Dark backgrounds, selected states |
| `--bg-muted` | #27272A | Muted/secondary dark backgrounds |
| `--bg-primary` | #FFFFFF | Primary surface background |
| `--bg-surface` | #F4F4F5 | Card/component backgrounds |
| `--text-primary` | #000000 | Primary text |
| `--text-secondary` | #71717A | Secondary/muted text |
| `--text-tertiary` | #A1A1AA | Placeholder, hint text |
| `--text-inverted` | #FFFFFF | Text on dark backgrounds |
| `--text-disabled` | #D4D4D8 | Disabled state text |
| `--border-strong` | #E4E4E7 | Primary borders |
| `--border-subtle` | #F4F4F5 | Subtle separators |
| `--color-success` | #22C55E | Success states, positive amounts |
| `--color-success-bg` | #DCFCE7 | Success background |
| `--color-error` | #EF4444 | Error/destructive states |
| `--color-error-bg` | #FEE2E2 | Error background |
| `--color-warning` | #F59E0B | Warning states |
| `--color-warning-bg` | #FEF3C7 | Warning background |
| `--color-info` | #3B82F6 | Informational states |
| `--color-info-bg` | #DBEAFE | Info background |

### 2.2 Transaction State Colors

| State | Background | Text | Border | Icon |
|-------|------------|------|--------|------|
| **Confirmed** | `--bg-surface` | `--text-primary` | `--border-strong` | None |
| **Pending** | `--color-warning-bg` | `--color-warning` | `--color-warning` | Clock icon |
| **Overdue** | `--color-error-bg` | `--color-error` | `--color-error` | Warning icon |
| **Success** | `--color-success-bg` | `--color-success` | None | Check icon |
| **Error** | `--color-error-bg` | `--color-error` | None | Alert circle icon |
| **Info** | `--color-info-bg` | `--color-info` | None | Info icon |

### 2.3 Calendar Grid Colors

| Token | Hex Code | Usage |
|-------|----------|-------|
| `color-day-selected` | `--bg-dark` | Active/selected day (dark mode style) |
| `color-day-selected-text` | `--text-inverted` | Text on selected day |
| `color-day-today` | `--color-success` @ 20% opacity | Current date highlight |

---

## 3. Typography System

### 3.1 Font Families

The design system uses **Outfit** for headings and display text, and **Inter** for body text:

| Role | Font Family | Usage |
|------|-------------|-------|
| Display | Outfit | Headlines, hero amounts, section titles |
| Body | Inter | Body text, labels, descriptions, form inputs |

### 3.2 Type Scale

| Token | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `font-display-large` | 72pt | Bold (900) | - | Net worth hero amount |
| `font-display-medium` | 56pt | Bold (900) | - | Safe to Spend amount |
| `font-headline` | 40pt | Bold (800) | - | Page headers |
| `font-title` | 32pt | Bold (800) | - | Calendar month title |
| `font-section` | 24pt | Bold (800) | - | Section headers |
| `font-card-title` | 20pt | Bold (800) | - | Card titles |
| `font-body-large` | 16pt | Semi-Bold (600) | - | Transaction titles |
| `font-body-medium` | 14pt | Medium (500) | - | Body text, labels |
| `font-body-small` | 12pt | Normal (400) | - | Secondary info, metadata |
| `font-caption` | 11pt | Semi-Bold (600) | - | Badges, uppercase labels |
| `font-calendar-day` | 32pt | Bold (900) | - | Calendar day numbers |
| `font-calendar-amount` | 11pt | Semi-Bold (600) | - | Daily summary amounts |

### 3.3 Amount Display Typography

Financial amounts receive special typographic treatment to ensure immediate readability:

| Type | Style | Color | Example |
|------|-------|-------|---------|
| Positive Amount | `font-calendar-amount`, Bold | `--color-success` | +$1,000 |
| Negative Amount | `font-calendar-amount`, Bold | `--color-error` | -$80 |
| Safe to Spend | `font-display-medium`, Bold | `--text-primary` | $2,450.00 |
| Net Worth | `font-display-large`, Bold | `--text-primary` | $350,000 |
| Transaction Amount | `font-body-large`, SemiBold | Contextual | -$45.99 |

---

## 4. Spacing System

### 4.1 Base Unit

The spacing system uses a 4dp/4pt base unit, ensuring consistent rhythm throughout the interface.

| Token | Value | Usage |
|-------|-------|-------|
| `--spacing-1` | 4dp/pt | Tight internal padding |
| `--spacing-2` | 8dp/pt | Default internal padding |
| `--spacing-3` | 12dp/pt | Comfortable spacing |
| `--spacing-4` | 16dp/pt | Standard padding |
| `--spacing-5` | 20dp/pt | Section spacing |
| `--spacing-6` | 24dp/pt | Large section gaps |
| `--spacing-8` | 32dp/pt | Screen margins |
| `--spacing-10` | 40dp/pt | Major section separators |
| `--spacing-12` | 48dp/pt | Extra large spacing |

### 4.2 Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-sm` | 4dp/pt | Small elements |
| `--radius-md` | 8dp/pt | Checkboxes, small buttons |
| `--radius-lg` | 12dp/pt | Badges, inputs |
| `--radius-xl` | 16dp/pt | Cards, modals |
| `--radius-2xl` | 20dp/pt | Large cards |
| `--radius-full` | 9999dp/pt | Pills, avatars |

### 4.3 Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | 0 1px 2px rgba(0,0,0,0.05) | Subtle elevation |
| `--shadow-md` | 0 4px 6px rgba(0,0,0,0.1) | Cards, dropdowns |
| `--shadow-lg` | 0 10px 15px rgba(0,0,0,0.1) | Modals, dialogs |
| `--shadow-xl` | 0 20px 25px rgba(0,0,0,0.15) | Heavy elevation |

### 4.4 Component Spacing

| Context | Spacing | Usage |
|---------|---------|-------|
| Card Internal | `--spacing-4` | Content padding within cards |
| List Item | `--spacing-3` vertical | Vertical gap between list items |
| Button Group | `--spacing-2` | Horizontal gap between buttons |
| Form Fields | `--spacing-4` | Vertical gap between form inputs |
| Section Header | `--spacing-5` | Gap above section headers |

---

## 5. Component Library

### 5.1 Reusable Component Overview

The design system contains **62 reusable components**, organized into categories for easy reference.

| # | Component ID | Component Name | Category |
|---|--------------|----------------|----------|
| 1 | AbKW9 | Day Wrapper / Active | Calendar |
| 2 | sqaBa | Day Wrapper / Default | Calendar |
| 3 | Ks9uf | Transaction Item | Transaction |
| 4 | 0N6DT | Schedule Card | Transaction |
| 5 | XOueE | Card / Dark | Account |
| 6 | iDE7I | Card / Light | Account |
| 7 | 0yIOD | Hero / Safe to Spend | Metrics |
| 8 | UexMB | Hero / Net Worth | Metrics |
| 9 | mTtvR | Segmented Control | Common |
| 10 | mbxe4 | Button / Primary | Buttons |
| 11 | xR72O | Button / Outline | Buttons |
| 12 | MO989 | Button / Ghost | Buttons |
| 13 | Ifqk0 | Button / Destructive | Buttons |
| 14 | Bdc8O | Button / Primary Icon Leading | Buttons |
| 15 | m39bO | Button / Outline Icon Trailing | Buttons |
| 16 | HVMad | Search Bar | Common |
| 17 | CvyNk | Header | Layout |
| 18 | O3ux1 | Section Header | Layout |
| 19 | JxzdM | Accordion Item | Common |
| 20 | c9XFF | Input Group | Form |
| 21 | 4SRSD | Checkbox Row | Form |
| 22 | L2zpZ | Form / Large Amount Input | Form |
| 23 | auccy | Textarea Group | Form Extended |
| 24 | tA0BH | Toggle Switch | Form Extended |
| 25 | iN2jx | Radio Group | Form Extended |
| 26 | VWxgC | Radio Item | Form Extended |
| 27 | 8G9pp | Select Group | Form Extended |
| 28 | qpBrt | Number Input | Form Extended |
| 29 | EZOJk | Tab Bar Pill | Navigation |
| 30 | hpkIJ | Tabs / Vertical | Navigation |
| 31 | 7NUcN | Breadcrumbs | Navigation |
| 32 | Z8E9a | Sidebar Navigation | Navigation |
| 33 | CVw4C | Badge / Pending | Status |
| 34 | ywlyv | Badge / Overdue | Status |
| 35 | bHBso | Badge / Success | Status |
| 36 | pRGld | Badge / Warning | Status |
| 37 | b4SK9 | Badge / Error | Status |
| 38 | PkcO9 | Badge / Info | Status |
| 39 | Ksdjt | Simulation Form | Sandbox |
| 40 | qE6i6 | Results List | Sandbox |
| 41 | QqCcu | Status Bar | Layout |
| 42 | lktGa | Metrics Section | Layout |
| 43 | QkEEz | Calendar Section | Layout |
| 44 | K6q45 | Schedule Section | Layout |
| 45 | cWjai | Calendar Header | Layout |
| 46 | JFDlk | Calendar Metrics | Layout |
| 47 | oMq1b | Transaction Header | Layout |
| 48 | sC3BH | Pool Section | Layout |
| 49 | wyzGY | Wall Section | Layout |
| 50 | 1Lvt4 | Credit Card Section | Layout |
| 51 | hYiWT | Metrics Row | Layout |
| 52 | X1K6P | Stat Card | Data Display |
| 53 | WdLZn | Progress Bar | Data Display |
| 54 | jsUSG | Avatar Group | Data Display |
| 55 | VlWN4 | Avatar | Data Display |
| 56 | mhCFr | List Container | Lists |
| 57 | rJTFv | List Item | Lists |
| 58 | CrHoZ | Table Wrapper | Lists |
| 59 | TFM6d | Modal / Dialog | Feedback |
| 60 | tCFvy | Alert / Success | Feedback |
| 61 | GdTRQ | Alert / Warning | Feedback |
| 62 | 20TKu | Alert / Error | Feedback |
| 63 | 9teyP | Alert / Info | Feedback |
| 64 | TzE0w | Toast / Success | Feedback |
| 65 | 8CxCm | Tooltip | Feedback |
| 66 | 0JGwC | Confirm Dialog | Feedback |
| 67 | 8duML | Divider | Utilities |
| 68 | 4ftwe | Divider / Vertical | Utilities |
| 69 | SNgxx | Skeleton / Avatar Row | Utilities |
| 70 | wCzH5 | Pagination | Utilities |
| 71 | S7FdH | Transaction List / Mobile | Mobile |

---

### 5.2 Calendar Components

#### Day Wrapper / Active (`AbKW9`)

Active state for calendar day cells with highlighted background.

**Visual Specifications:**
- Background: `--bg-dark`
- Corner radius: `--radius-lg` (12px)
- Padding: 8px horizontal, 10px vertical
- Day text: 32pt, font-weight 900, `--text-inverted`, Outfit font
- Dot indicator: 6x6 ellipse, `--text-inverted`

#### Day Wrapper / Default (`sqaBa`)

Default state for calendar day cells without selection.

**Visual Specifications:**
- Background: Transparent
- Corner radius: `--radius-lg` (12px)
- Day text: 32pt, font-weight 500, `--text-secondary`, Outfit font
- Dot indicator: 6x6 ellipse, `--text-secondary`

---

### 5.3 Transaction Components

#### Transaction Item (`Ks9uf`)

Row item displaying a single transaction with status, details, and amount.

**Visual Specifications:**
- Layout: Horizontal, `--spacing-4` gap
- Padding: `--spacing-4` all sides
- Status indicator: 24x24 circle with border
- Title: 16pt, font-weight 600, `--text-primary`, Outfit font
- Meta: 12pt, font-weight normal, `--text-tertiary`, Inter font
- Amount: 20pt, font-weight 800, `--text-primary`, Outfit font

#### Schedule Card (`0N6DT`)

Card component for displaying scheduled/future transactions.

**Visual Specifications:**
- Layout: Horizontal, `--spacing-4` gap
- Background: `--bg-surface`
- Corner radius: `--radius-xl` (16px)
- Accent bar: 4px wide, 48px tall, `--bg-dark`
- Date: 24pt, font-weight 800, `--text-primary`, Outfit font
- Title: 16pt, font-weight 600, `--text-primary`
- Meta: 12pt, font-weight normal, `--text-secondary`

#### Transaction List / Mobile (`S7FdH`)

Mobile-optimized list of transactions.

**Visual Specifications:**
- Layout: Vertical, `--spacing-2` gap
- Card corner radius: `--radius-lg` (12px)
- Card padding: `--spacing-4`
- Footer height: 44px

---

### 5.4 Account Components

#### Card / Dark (`XOueE`)

Dark-themed account card for spending pools and checking accounts.

**Visual Specifications:**
- Width: 180px (fixed)
- Background: `--bg-dark`
- Corner radius: `--radius-2xl` (20px)
- Padding: `--spacing-4`
- Badge: `--radius-lg` corner radius, `--bg-primary` background, 11pt, uppercase
- Amount: 24pt, font-weight 800, `--text-inverted`, Outfit font
- Description: 12pt, normal weight, `--text-tertiary`, Inter font

#### Card / Light (`iDE7I`)

Light-themed account card for savings and investments.

**Visual Specifications:**
- Width: 180px (fixed)
- Background: `--bg-surface`
- Corner radius: `--radius-2xl` (20px)
- Padding: `--spacing-4`
- Badge: `--radius-lg` corner radius, `--bg-dark` background, 11pt, uppercase
- Amount: 24pt, font-weight 800, `--text-primary`, Outfit font
- Description: 12pt, normal weight, `--text-secondary`, Inter font

---

### 5.5 Metrics Components

#### Hero / Safe to Spend (`0yIOD`)

Primary metrics display for available spending funds.

**Visual Specifications:**
- Layout: Vertical, 6px gap
- Label: 14pt, font-weight 600, uppercase, letter-spacing 2px, `--text-secondary`, Outfit font
- Primary amount: 56pt, font-weight 900, letter-spacing -2px, `--text-primary`, Outfit font
- Daily rate: 24pt, font-weight 300, letter-spacing -0.5px, `--text-primary`, Outfit font

#### Hero / Net Worth (`UexMB`)

Total net worth display component.

**Visual Specifications:**
- Layout: Vertical, 6px gap
- Label: 13pt, font-weight 600, uppercase, letter-spacing 2px, `--text-secondary`, Outfit font
- Amount: 72pt, font-weight 900, letter-spacing -3px, `--text-primary`, Outfit font

---

### 5.6 Common Components

#### Segmented Control (`mTtvR`)

Toggle control for switching between two options (e.g., Live Budget / Sandbox).

**Visual Specifications:**
- Height: 48px
- Background: `--bg-surface`
- Corner radius: `--radius-full` (24px, pill shape)
- Gap: 4px
- Padding: 4px all sides
- Active segment: `--radius-lg` corner radius, `--bg-dark` background, white text
- Inactive segment: transparent background, `--text-secondary` text

#### Search Bar (`HVMad`)

Search input component with icon and placeholder.

**Visual Specifications:**
- Width: 340px (default)
- Height: 48px
- Corner radius: `--radius-full` (24px)
- Background: `--bg-surface`
- Gap: 12px
- Padding: 12px horizontal, 16px vertical
- Icon: 18x18, lucide search icon, `--text-tertiary`
- Text: 14pt, normal weight, `--text-tertiary`, Inter font

#### Header (`CvyNk`)

Page header with title and action button.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Horizontal, space-between alignment
- Title: 40pt, font-weight 800, letter-spacing -1px, `--text-primary`, Outfit font
- Icon button: 44x44px, `--radius-lg` corner radius, `--bg-surface` background

#### Section Header (`O3ux1`)

Section title with optional action text.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Horizontal, space-between, bottom-aligned
- Title: 24pt, font-weight 800, letter-spacing -0.5px, `--text-primary`, Outfit font
- Action: 14pt, font-weight 500, `--text-secondary`, Inter font

#### Accordion Item (`JxzdM`)

Expandable/collapsible list item with chevron indicator.

**Visual Specifications:**
- Width: 340px (default)
- Background: `--bg-surface`
- Corner radius: `--radius-xl` (16px)
- Padding: `--spacing-4` all sides
- Title: 15pt, font-weight 600, `--text-primary`, Outfit font
- Chevron: 20x20, lucide chevron-right, `--text-tertiary`

#### Input Group (`c9XFF`)

Form input with label.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Vertical, 8px gap
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Input field: 48px height, `--radius-lg` corner radius, 1.5px border
- Input text: 14pt, normal weight, `--text-tertiary`, Inter font

#### Checkbox Row (`4SRSD`)

Form checkbox with label.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Horizontal, 12px gap
- Checkbox: 24x24px, `--radius-md` corner radius, 2px border
- Label: 14pt, font-weight 500, `--text-primary`, Inter font

---

### 5.7 Button Components

#### Button / Primary (`mbxe4`)

Primary action button with filled background.

**Visual Specifications:**
- Height: 48px
- Corner radius: `--radius-full` (24px, pill shape)
- Background: `--bg-dark`
- Text: 16pt, font-weight 700, `--text-inverted`, Outfit font
- Padding: 0px horizontal, 24px vertical

#### Button / Outline (`xR72O`)

Secondary action button with outlined style.

**Visual Specifications:**
- Height: 48px
- Corner radius: `--radius-full` (24px)
- Border: 2px, `--border-strong`
- Background: Transparent
- Text: 16pt, font-weight 700, `--text-primary`, Outfit font
- Padding: 0px horizontal, 24px vertical

#### Button / Ghost (`MO989`)

Ghost button with text and optional icon, no background.

**Visual Specifications:**
- Height: 48px
- Padding: 0px horizontal, 20px vertical
- Icon: 18x18, lucide plus, `--text-primary`
- Gap: 8px between icon and text
- Text: 16pt, font-weight 700, `--text-primary`, Outfit font

#### Button / Destructive (`Ifqk0`)

Destructive action button with error styling.

**Visual Specifications:**
- Height: 48px
- Corner radius: `--radius-full` (24px)
- Background: `--color-error`
- Icon: 18x18, lucide trash-2, `--text-inverted`
- Text: 16pt, font-weight 700, `--text-inverted`, Outfit font
- Padding: 0px horizontal, 24px vertical

#### Button / Primary Icon Leading (`Bdc8O`)

Primary button with icon leading the text.

**Visual Specifications:**
- Height: 48px
- Corner radius: `--radius-full` (24px)
- Background: `--bg-dark`
- Gap: 10px
- Icon: 18x18, lucide plus, `--text-inverted`
- Text: 16pt, font-weight 700, `--text-inverted`, Outfit font
- Padding: 0px horizontal, 24px vertical

#### Button / Outline Icon Trailing (`m39bO`)

Outline button with icon trailing the text.

**Visual Specifications:**
- Height: 48px
- Corner radius: `--radius-full` (24px)
- Border: 2px, `--border-strong`
- Gap: 8px
- Icon: 18x18, lucide arrow-right, `--text-primary`
- Text: 16pt, font-weight 700, `--text-primary`, Outfit font
- Padding: 0px horizontal, 24px vertical

---

### 5.8 Form Extended Components

#### Textarea Group (`auccy`)

Multi-line text input with label.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Vertical, 8px gap
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Textarea: 96px height, `--radius-lg` corner radius, 1.5px border
- Padding: 12px horizontal, 16px vertical

#### Toggle Switch (`tA0BH`)

Toggle control for boolean settings.

**Visual Specifications:**
- Layout: Horizontal, 12px gap
- Track: 44x24px, `--radius-lg` corner radius, `--bg-dark`
- Thumb: 20x20px, `--radius-full`, `--text-inverted`
- Label: 14pt, font-weight 500, `--text-primary`, Inter font

#### Radio Group (`iN2jx`)

Container for radio button options.

**Visual Specifications:**
- Layout: Vertical
- Gap: 12px

#### Radio Item (`VWxgC`)

Single radio button option.

**Visual Specifications:**
- Layout: Horizontal, 12px gap
- Circle: 24x24px, `--radius-lg` corner radius, 2px border
- Inner: 16x16px, `--radius-md`, `--bg-dark`
- Label: 14pt, font-weight 500, `--text-primary`, Inter font

#### Select Group (`8G9pp`)

Dropdown select with label.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Vertical, 8px gap
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Trigger: 48px height, `--radius-lg` corner radius, `--bg-surface`, 1.5px border
- Padding: 12px horizontal, 16px vertical
- Layout: space-between alignment

#### Number Input (`qpBrt`)

Numeric input field.

**Visual Specifications:**
- Width: 200px (default)
- Layout: Vertical, 8px gap
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Input: 48px height, `--radius-lg` corner radius, `--bg-surface`, 1.5px border
- Padding: 12px horizontal, 16px vertical

#### Form / Large Amount Input (`L2zpZ`)

Prominent amount input for transactions.

**Visual Specifications:**
- Layout: Vertical, centered, 8px gap
- Label: 13pt, font-weight 600, uppercase, letter-spacing 2px, `--text-secondary`, Inter font
- Amount: 72pt, font-weight 900, letter-spacing -3px, `--text-primary`, Outfit font

---

### 5.9 Navigation Components

#### Tab Bar Pill (`EZOJk`)

Bottom navigation bar with tabs.

**Visual Specifications:**
- Width: 340px (default)
- Height: 62px
- Background: `--bg-surface`
- Corner radius: `--radius-full` (32px)
- Padding: 4px
- Layout: space-around
- Tab item padding: 8px horizontal, 14px vertical
- Add button: 56x56px, `--radius-full`, `--bg-dark`
- Active tab: `--text-primary`, 700 weight, 11pt
- Inactive tab: `--text-tertiary`, 500 weight, 11pt

#### Tabs / Vertical (`hpkIJ`)

Vertical tab navigation component.

**Visual Specifications:**
- Width: 200px (default)
- Background: `--bg-surface`
- Corner radius: `--radius-xl` (16px)
- Padding: 8px
- Gap: 4px between tabs
- Layout: Vertical
- Active tab: `--radius-lg` corner radius, `--bg-dark`, `--text-inverted`
- Inactive tab: transparent background, `--text-primary`
- Tab height: 44px
- Gap: 10px between icon and label

#### Breadcrumbs (`7NUcN`)

Breadcrumb navigation trail.

**Visual Specifications:**
- Layout: Horizontal, center aligned
- Gap: 8px
- Item: 14pt, font-weight 500, `--text-secondary`, Inter font
- Separator: `--text-tertiary`
- Active item: `--text-primary`, font-weight 600

#### Sidebar Navigation (`Z8E9a`)

Sidebar navigation menu.

**Visual Specifications:**
- Width: 260px (default)
- Background: `--bg-surface`
- Corner radius: `--radius-2xl` (20px)
- Padding: 12px
- Gap: 4px between items
- Section label: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--text-tertiary`, Inter font
- Item height: 44px
- Item corner radius: `--radius-lg` (12px)
- Item padding: 10px horizontal, 14px vertical
- Item gap: 12px between icon and label
- Active item: `--bg-dark` background, `--text-inverted`

---

### 5.10 Status Components

#### Badge / Pending (`CVw4C`)

Status badge for pending transactions.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Border: 1.5px, `--border-strong`
- Background: `--bg-surface`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--text-secondary`, Outfit font
- Padding: 4px vertical, 10px horizontal

#### Badge / Overdue (`ywlyv`)

Status badge for overdue transactions.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Background: `--bg-dark`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--text-inverted`, Outfit font
- Padding: 4px vertical, 10px horizontal

#### Badge / Success (`bHBso`)

Status badge for completed/successful states.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Background: `--color-success-bg`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--color-success`, Outfit font
- Padding: 4px vertical, 10px horizontal

#### Badge / Warning (`pRGld`)

Status badge for warning states.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Background: `--color-warning-bg`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--color-warning`, Outfit font
- Padding: 4px vertical, 10px horizontal

#### Badge / Error (`b4SK9`)

Status badge for error states.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Background: `--color-error-bg`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--color-error`, Outfit font
- Padding: 4px vertical, 10px horizontal

#### Badge / Info (`PkcO9`)

Status badge for informational states.

**Visual Specifications:**
- Corner radius: `--radius-lg` (12px)
- Background: `--color-info-bg`
- Text: 11pt, font-weight 600, uppercase, letter-spacing 1px, `--color-info`, Outfit font
- Padding: 4px vertical, 10px horizontal

---

### 5.11 Feedback Components

#### Modal / Dialog (`TFM6d`)

Modal dialog overlay.

**Visual Specifications:**
- Width: 400px (default)
- Background: `--bg-primary`
- Corner radius: `--radius-2xl` (20px)
- Padding: 24px all sides
- Gap: 24px
- Layout: Vertical
- Header height: 32px
- Action layout: end-aligned, 12px gap

#### Confirm Dialog (`0JGwC`)

Confirmation dialog with icon, title, description, and actions.

**Visual Specifications:**
- Width: 380px (default)
- Background: `--bg-primary`
- Corner radius: `--radius-2xl` (20px)
- Padding: 24px all sides
- Gap: 20px
- Layout: Vertical
- Icon: 56x56px, `--radius-full`, `--color-error-bg`
- Title: 18pt, font-weight 700, `--text-primary`, Outfit font
- Description: 14pt, normal weight, `--text-secondary`, Inter font
- Actions: end-aligned, 12px gap

#### Alert / Success (`tCFvy`)

Success alert banner.

**Visual Specifications:**
- Background: `--color-success-bg`
- Corner radius: `--radius-lg` (12px)
- Padding: 16px vertical, 20px horizontal
- Gap: 12px
- Icon: 20x20, lucide check-circle, `--color-success`
- Text: 14pt, font-weight 500, `--color-success`, Inter font

#### Alert / Warning (`GdTRQ`)

Warning alert banner.

**Visual Specifications:**
- Background: `--color-warning-bg`
- Corner radius: `--radius-lg` (12px)
- Padding: 16px vertical, 20px horizontal
- Gap: 12px
- Icon: 20x20, lucide alert-triangle, `--color-warning`
- Text: 14pt, font-weight 500, `--color-warning`, Inter font

#### Alert / Error (`20TKu`)

Error alert banner.

**Visual Specifications:**
- Background: `--color-error-bg`
- Corner radius: `--radius-lg` (12px)
- Padding: 16px vertical, 20px horizontal
- Gap: 12px
- Icon: 20x20, lucide alert-circle, `--color-error`
- Text: 14pt, font-weight 500, `--color-error`, Inter font

#### Alert / Info (`9teyP`)

Informational alert banner.

**Visual Specifications:**
- Background: `--color-info-bg`
- Corner radius: `--radius-lg` (12px)
- Padding: 16px vertical, 20px horizontal
- Gap: 12px
- Icon: 20x20, lucide info, `--color-info`
- Text: 14pt, font-weight 500, `--color-info`, Inter font

#### Toast / Success (`TzE0w`)

Success toast notification.

**Visual Specifications:**
- Width: 320px (default)
- Background: `--bg-dark`
- Corner radius: `--radius-lg` (12px)
- Padding: 16px vertical, 20px horizontal
- Gap: 12px
- Icon: 20x20, lucide check, `--text-inverted`
- Text: 14pt, font-weight 500, `--text-inverted`, Inter font

#### Tooltip (`8CxCm`)

Tooltip for contextual information.

**Visual Specifications:**
- Background: `--bg-dark`
- Corner radius: `--radius-md` (8px)
- Padding: 8px vertical, 12px horizontal
- Text: 12pt, font-weight 500, `--text-inverted`, Inter font

---

### 5.12 Data Display Components

#### Stat Card (`X1K6P`)

Statistics display card with value and trend.

**Visual Specifications:**
- Width: 180px (default)
- Background: `--bg-surface`
- Corner radius: `--radius-2xl` (20px)
- Padding: 20px all sides
- Gap: 8px
- Layout: Vertical
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Value: 28pt, font-weight 800, `--text-primary`, Outfit font
- Trend: 13pt, font-weight 600, `--color-success`, Inter font

#### Progress Bar (`WdLZn`)

Progress indicator with label and value.

**Visual Specifications:**
- Width: 340px (default)
- Layout: Vertical, 8px gap
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Track: 8px height, `--radius-sm` corner radius, `--bg-surface`, 1px border
- Value: 13pt, font-weight 600, `--text-primary`, Inter font

#### Avatar (`VlWN4`)

User avatar component.

**Visual Specifications:**
- Sizes: 32px (small), 40px (medium), 56px (large)
- Corner radius: `--radius-full` (circular)
- Background: `--bg-dark` or `--color-error` for different users

#### Avatar Group (`jsUSG`)

Group of overlapping avatars.

**Visual Specifications:**
- Layout: Horizontal
- Avatar size: 32px
- Overlap: -8px margin (negative gap)
- Corner radius: `--radius-full` (circular)

---

### 5.13 List Components

#### List Container (`mhCFr`)

Container for list items with consistent styling.

**Visual Specifications:**
- Layout: Vertical
- Gap: 2px between items

#### List Item (`rJTFv`)

Standard list item with icon, text, and chevron.

**Visual Specifications:**
- Height: 48px
- Background: `--bg-surface`
- Corner radius: `--radius-md` (8px)
- Padding: 0px vertical, 16px horizontal
- Layout: Horizontal, center aligned
- Gap: 12px
- Icon: 20x20, lucide folder, `--text-primary`
- Text: 14pt, font-weight 500, `--text-primary`, Inter font
- Chevron: 20x20, lucide chevron-right, `--text-tertiary`

#### Table Wrapper (`CrHoZ`)

Wrapper for table content.

**Visual Specifications:**
- Layout: Vertical
- Gap: 2px

---

### 5.14 Layout Components

#### Status Bar (`QqCcu`)

Device status bar component.

**Visual Specifications:**
- Height: 44px
- Padding: 12px vertical, 24px horizontal
- Layout: Horizontal, space-between alignment
- Time display and system icons

#### Metrics Section (`lktGa`)

Section container for metrics display.

**Visual Specifications:**
- Layout: Vertical
- Gap: 6px
- Label: 14pt, font-weight 600, uppercase, letter-spacing 2px, `--text-secondary`, Outfit font

#### Calendar Section (`QkEEz`)

Section container for calendar grid.

**Visual Specifications:**
- Layout: Vertical, 20px gap
- Grid padding: 0px vertical, 8px horizontal

#### Schedule Section (`K6q45`)

Section container for scheduled transactions.

**Visual Specifications:**
- Layout: Vertical, 20px gap

#### Calendar Header (`cWjai`)

Header for calendar view with month/year and navigation.

**Visual Specifications:**
- Layout: Horizontal, space-between alignment
- Title: 32pt, font-weight 800, letter-spacing -1px, `--text-primary`, Outfit font
- Action gap: 16px

#### Calendar Metrics (`JFDlk`)

Inline metrics display within calendar.

**Visual Specifications:**
- Background: `--bg-surface`
- Corner radius: `--radius-lg` (12px)
- Padding: 12px vertical, 16px horizontal
- Gap: 8px
- Layout: Horizontal, space-between alignment
- Label: 13pt, font-weight 500, `--text-secondary`, Inter font
- Value: 16pt, font-weight 800, `--text-primary`, Outfit font

#### Transaction Header (`oMq1b`)

Header for transaction list section.

**Visual Specifications:**
- Padding: 24px top, 0px sides, 8px bottom
- Gap: 12px
- Text: 20pt, font-weight 800, `--text-primary`, Outfit font

#### Pool Section (`sC3BH`)

Section for spending pool accounts.

**Visual Specifications:**
- Layout: Vertical, 16px gap

#### Wall Section (`wyzGY`)

Section for walled garden accounts.

**Visual Specifications:**
- Layout: Vertical, 16px gap

#### Credit Card Section (`1Lvt4`)

Section for credit card accounts.

**Visual Specifications:**
- Layout: Vertical, 16px gap

#### Metrics Row (`hYiWT`)

Horizontal row for multiple metric items.

**Visual Specifications:**
- Layout: Horizontal
- Gap: 12px
- Each side: Vertical layout with 6px gap

---

### 5.15 Sandbox Components

#### Simulation Form (`Ksdjt`)

Simulation input form with multiple fields.

**Visual Specifications:**
- Background: `--bg-surface`
- Corner radius: `--radius-xl` (16px)
- Padding: 24px all sides
- Gap: 16px
- Layout: Vertical

#### Results List (`qE6i6`)

Results list showing simulation impacts.

**Visual Specifications:**
- Background: `--bg-surface`
- Corner radius: `--radius-xl` (16px)
- Layout: Vertical (no internal padding)
- Dividers: 1px, `--border-strong`

---

### 5.16 Utility Components

#### Divider (`8duML`)

Horizontal divider line.

**Visual Specifications:**
- Height: 1px
- Color: `--border-strong`

#### Divider / Vertical (`4ftwe`)

Vertical divider line.

**Visual Specifications:**
- Width: 1px
- Color: `--border-strong`

#### Skeleton / Avatar Row (`SNgxx`)

Loading skeleton with avatar and text lines.

**Visual Specifications:**
- Layout: Horizontal, 12px gap
- Avatar: 40x40px, `--radius-full`, `--bg-surface`
- Lines: Vertical layout, 8px gap, `--bg-surface`

#### Pagination (`wCzH5`)

Pagination control for lists.

**Visual Specifications:**
- Layout: Horizontal, 8px gap
- Page button: 36px height, `--radius-md` corner radius, 8px padding
- Active page: `--bg-dark` background, `--text-inverted`
- Inactive page: transparent background, 1px border
- Ellipsis: `--text-tertiary`

---

## 6. Screen Layouts

### 6.1 Main Dashboard Screen

The primary screen displaying the budget overview with metrics, calendar, and transactions.

```
┌─────────────────────────────────────────┐
│  [Status Bar]                            │
│  9:41              [Cell] [WiFi] [Bat]  │
├─────────────────────────────────────────┤
│  [Header]                                │
│  Budget                        [Bell]    │
├─────────────────────────────────────────┤
│  [Search Bar]                            │
│  ┌─────────────────────────────────────┐ │
│  │ Q  Search...                        │ │
│  └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│  [Hero / Safe to Spend]                 │
│  SAFE TO SPEND                          │
│  ₱15,000                    ₱500/day     │
├─────────────────────────────────────────┤
│  [Segmented Control]                     │
│  ┌─────────────┬─────────────┐          │
│  │ Live Budget │  Sandbox    │          │
│  └─────────────┴─────────────┘          │
├─────────────────────────────────────────┤
│  [Calendar Section]                     │
│  This Week           ₱2,500 spent       │
│  ┌───┬───┬───┬───┬───┬───┬───┐        │
│  │ M │ T │ W │ T │ F │ S │ S │        │
│  └───┴───┴───┴───┴───┴───┴───┘        │
├─────────────────────────────────────────┤
│  [Schedule Section]                     │
│  Overdue                 2 Pending      │
│  ┌─────────────────────────────────────┐ │
│  │ ● PLDT Home Fibr   -₱1,599          │ │
│  │   Yesterday · ₱1,599.00            │ │
│  ├─────────────────────────────────────┤ │
│  │ ● Netflix          -₱499            │ │
│  │   3 Days Ago · ₱499.00             │ │
│  └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│  [Tab Bar Pill]                          │
│  [Home] [Calendar] [+] [Accounts] [Me]  │
└─────────────────────────────────────────┘
```

---

## 7. Design System Metadata

### 7.1 Version Information

| Property | Value |
|----------|-------|
| Total Reusable Components | 62 |
| Previous Count | 29 |
| New Components Added | 33 |
| Audit Status | Completed |
| Last Updated | March 2026 |

### 7.2 Component Categories Summary

| Category | Count | Components |
|----------|-------|------------|
| Calendar | 2 | Day Wrapper/Active, Day Wrapper/Default |
| Transaction | 3 | Transaction Item, Schedule Card, Transaction List/Mobile |
| Account | 2 | Card/Dark, Card/Light |
| Metrics | 2 | Hero/Safe to Spend, Hero/Net Worth |
| Buttons | 6 | Button/Primary, Button/Outline, Button/Ghost, Button/Destructive, Button/Primary Icon Leading, Button/Outline Icon Trailing |
| Common | 6 | Segmented Control, Search Bar, Header, Section Header, Accordion Item, Input Group, Checkbox Row |
| Form | 7 | Input Group, Checkbox Row, Form/Large Amount Input, Textarea Group, Toggle Switch, Radio Group, Radio Item, Select Group, Number Input |
| Navigation | 4 | Tab Bar Pill, Tabs/Vertical, Breadcrumbs, Sidebar Navigation |
| Status | 6 | Badge/Pending, Badge/Overdue, Badge/Success, Badge/Warning, Badge/Error, Badge/Info |
| Feedback | 7 | Modal/Dialog, Confirm Dialog, Alert/Success, Alert/Warning, Alert/Error, Alert/Info, Toast/Success, Tooltip |
| Data Display | 4 | Stat Card, Progress Bar, Avatar Group, Avatar |
| Lists | 3 | List Container, List Item, Table Wrapper |
| Layout | 11 | Status Bar, Metrics Section, Calendar Section, Schedule Section, Calendar Header, Calendar Metrics, Transaction Header, Pool Section, Wall Section, Credit Card Section, Metrics Row |
| Sandbox | 2 | Simulation Form, Results List |
| Utilities | 4 | Divider, Divider/Vertical, Skeleton/Avatar Row, Pagination |

---

## 8. Appendix: Component Quick Reference

### Color Token Reference

| Token | Hex | Purpose |
|-------|-----|---------|
| `--bg-dark` | #000000 | Dark backgrounds, selected states |
| `--bg-surface` | #F4F4F5 | Card/component backgrounds |
| `--text-primary` | #000000 | Primary text |
| `--text-secondary` | #71717A | Secondary text |
| `--text-inverted` | #FFFFFF | Text on dark backgrounds |
| `--border-strong` | #E4E4E7 | Primary borders |
| `--color-success` | #22C55E | Success states |
| `--color-error` | #EF4444 | Error/destructive states |
| `--color-warning` | #F59E0B | Warning states |
| `--color-info` | #3B82F6 | Informational states |

### Typography Reference

| Token | Size | Weight | Font |
|-------|------|--------|------|
| `font-display-large` | 72pt | Bold (900) | Outfit |
| `font-display-medium` | 56pt | Bold (900) | Outfit |
| `font-title` | 32pt | Bold (800) | Outfit |
| `font-section` | 24pt | Bold (800) | Outfit |
| `font-body-large` | 16pt | Semi-Bold (600) | Inter |
| `font-body-medium` | 14pt | Medium (500) | Inter |
| `font-caption` | 11pt | Semi-Bold (600) | Outfit |

### Spacing Reference

| Token | Value |
|-------|-------|
| `--spacing-1` | 4dp/pt |
| `--spacing-2` | 8dp/pt |
| `--spacing-3` | 12dp/pt |
| `--spacing-4` | 16dp/pt |
| `--spacing-6` | 24dp/pt |
| `--spacing-8` | 32dp/pt |

### Radius Reference

| Token | Value |
|-------|-------|
| `--radius-md` | 8pt |
| `--radius-lg` | 12pt |
| `--radius-xl` | 16pt |
| `--radius-2xl` | 20pt |
| `--radius-full` | 9999pt (pill/circle) |

---

*Document Version: 3.0*  
*Last Updated: March 2026*  
*Maintainers: Design System Team*  
*Source: DESIGN_SYSTEM.pen (62 reusable components)*
