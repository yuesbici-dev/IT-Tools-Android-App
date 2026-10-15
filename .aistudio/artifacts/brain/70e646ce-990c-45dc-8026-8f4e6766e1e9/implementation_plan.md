# NetPulse - Modern IT Network Suite

A professional, ad-free IT network diagnostic and sysadmin suite for Android built with Jetpack Compose and Material 3, featuring customizable quick-access grid objects for tools and external apps, enhanced TCP/UDP port scanning, and structured IP address management.

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan has been expanded to incorporate your latest request:
> - **Editable Quick-Access Grid Objects & External App Launcher**:
>   - **Customizable Dashboard Grid**: Users can customize the home grid items: pin, unpin, reorder, edit labels, or add custom quick-access cards.
>   - **External App Quick Launch**: Add quick-access grid objects that launch companion external apps (e.g. Termux, ConnectBot/JuiceSSH, Wi-Fi Analyzer, WireGuard/OpenVPN, Router Web Consoles) via Android `PackageManager.getLaunchIntentForPackage` or URL deep links (`https://` or `ssh://`).
>   - **Edit & Add Modal**: Configure title, subtitle, icon selection (terminal, server, shield, router, code, link), and target intent (app package or URL).
> - **Enhanced Port Scanner**:
>   - Custom port ranges (`1-1024`, custom lists), TCP & UDP dual protocol selection, configurable connection timeouts (100ms - 3000ms), and detailed Open/Closed/Filtered status reports with service resolution and RTT.
> - **Categorized IP Address Book & Host Manager**:
>   - Individual IP address saving with custom labels, notes, category groups ('Home Network', 'Work Servers', 'IoT Devices'), live search, and one-tap diagnostic launching.

---

## 1. Overview & Core Concept

**NetPulse** is an ad-free, comprehensive network diagnostic suite and IT dashboard:

- **Dashboard & Quick Access**:
  - Customizable dense grid with category filter chips.
  - **Editable Quick Access Grid Objects**: Pre-configured with NetPulse native tools, plus the ability to add, edit, and launch external apps (Terminal, SSH, VPN, Router Consoles).
- **Diagnostics**:
  - **Ping Tool**: Continuous/counted ping, RTT sparkline, jitter, min/avg/max/loss metrics.
  - **Enhanced Port Scanner**: TCP & UDP scanning across customizable ranges (1-65535), configurable socket timeout, concurrency control, and open/closed status badges with service tagging.
  - **Traceroute**: Hop-by-hop latency and host reverse DNS resolution.
  - **NSLookup**: DNS record lookup (A, AAAA, CNAME, MX, TXT, NS, SOA) supporting custom saveable DNS resolvers (Google, Cloudflare, Quad9, or custom internal IPs).
  - **WHOIS & RDAP**: Domain and IP registration details, registrar, expiration, and raw records.
- **Local Network Discovery**:
  - **Network Information**: Internal IPv4/IPv6, gateway, subnet mask, active DNS servers, and public IP / ISP resolver.
  - **Subnet LAN Scanner**: High-speed parallel host discovery across local subnet with one-tap save to address book.
  - **Wake-on-LAN (WoL)**: Magic packet broadcaster with saved target MAC addresses.
- **Calculators**:
  - **IPv4 Subnet Calculator**: CIDR slider, netmask, wildcard, usable range, binary representation, and IP class.
  - **IPv6 Calculator**: Prefix length, expanded/compressed format, address count, and scope/type.
- **Feeds & Target Management**:
  - **IP Address Manager**: Full-featured host catalog with custom labels, notes, categories, search, and one-tap diagnostic actions.
  - **RSS Receiver**: Curated IT Security & sysadmin feeds (BleepingComputer, Hacker News, CISA, The Hacker News) with custom feed addition.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Home Screen with Editable Quick Access Grid**:
   - Top status header displaying active connection (Wi-Fi SSID or Cellular), current local IP pill, and edit mode toggle icon.
   - Category chips: **All**, **Diagnostics**, **Discovery**, **Calculators**, **IP Manager & Feeds**, **Custom Apps**.
   - **Dense Grid Layout**:
     - Standard tool cards (Ping, Port Scan, Net Info, LAN Scan, WoL, Traceroute, DNS, WHOIS, IP Calc v4/v6, RSS, IP Book).
     - Custom external app cards with distinctive "External" badge and custom icon.
   - **Quick Access Editor Dialog**:
     - Tap "Customize Grid" or "+ Add Quick Access Object":
       - Type: **Internal Tool** or **External App / URL**.
       - Title (e.g., "Termux", "Router Admin", "JuiceSSH").
       - Subtitle / Description (e.g., "Local Terminal", "192.168.1.1").
       - App Package Name (with popular presets like Termux, WireGuard, OpenVPN, ConnectBot) or Web/Custom URL.
       - Icon selection (Terminal, Shield, Router, Server, Web, Cloud, Key).
     - Long-press or edit button to re-order, hide, or delete custom grid objects.

2. **Enhanced Port Scanner Flow**:
   - Target input (pre-fillable from IP Manager or LAN scanner).
   - Scan Mode Toggle: **TCP** or **UDP**.
   - Port Range Input: Start/End port, or quick presets ("Well-Known 1-1024", "Web 80/443/8080", "Database", "Remote 22/3389/5900").
   - Timeout Slider: 100ms - 2000ms.
   - Live Scan Progress: Real-time progress bar, scanned counter, open port counter.
   - Results List:
     - Filter chips: **All Ports**, **Open Only**, **Closed Only**.
     - Service identification (e.g. `22: SSH`, `80: HTTP`, `443: HTTPS`, `3389: RDP`).
     - Latency in ms and Open/Closed status pills.

3. **IP Address Manager Flow**:
   - Search bar: Real-time search across label, IP, notes, and category.
   - Category filter carousel: "All", "Home Network", "Work Servers", "IoT Devices", "+ Add Category".
   - Host Card: Label, Monospace IP, Category pill, Notes preview, and Quick Action buttons (**Ping**, **Scan Ports**, **Wake**, **Edit**).
   - Dialog to add/edit targets with custom category selection and notes.

4. **Visual Identity & Theme**:
   - Modern Dark Slate background (`#0B0F19`, `#161F30`, `#1E293B`).
   - Vivid Sky Blue (`#38BDF8`) & Royal Indigo (`#2563EB`) accents.
   - Monospace typography for IPs, ports, MACs, and millisecond metrics.

---

## 3. Key Product Decisions & Trade-Offs

- **Editable Quick Access Persistence**:
  - *Chosen*: Room database table `QuickAccessItem` (id, title, subtitle, iconType, actionType, destinationTarget, isCustom, isVisible, orderIndex).
  - *Why*: Allows users to persist their custom layout and external app shortcuts across restarts without hardcoding.
- **External App Launch Handling**:
  - *Chosen*: Graceful intent launch via `context.packageManager.getLaunchIntentForPackage(packageName)` or `Intent(Intent.ACTION_VIEW, Uri.parse(url))` with user feedback toast if an app is not installed (with option to view in Play Store).
- **Port Scanner Concurrency Architecture**:
  - *Chosen*: Coroutine worker pool (50 parallel probes) with selective TCP socket connect and UDP datagram probe handling.
- **Relational IP Address Book**:
  - Room entities for `SavedHost` and `Category` with Flow-based reactive queries.

---

## 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│                           │                            │
│           NetPulseNavHost (Navigation Compose)         │
└───────────────────────────┬────────────────────────────┘
                            │
   ┌────────────────────────┴──────────────────────────┐
   ▼                                                   ▼
┌─────────────────────────┐               ┌──────────────────────────┐
│  ToolGridHomeScreen     │               │ Diagnostic Screens       │
│  - Category Filter Chips│               │ - PingScreen             │
│  - Connectivity Header  │               │ - EnhancedPortScanScreen │
│  - Editable Grid Cards  │               │ - NetworkInfoScreen      │
│  - External App Launcher│               │ - WakeOnLanScreen        │
│  - Add/Edit Grid Dialog │               │ - TracerouteScreen       │
└─────────────────────────┘               │ - DnsLookupScreen        │
                                          │ - WhoisScreen            │
                                          │ - LanScannerScreen       │
                                          │ - Ipv4CalculatorScreen   │
                                          │ - Ipv6CalculatorScreen   │
                                          │ - IpManagerScreen        │
                                          │ - RssReaderScreen        │
                                          └─────────────┬────────────┘
                                                        │
                      ┌─────────────────────────────────┴───────────────────┐
                      ▼                                                     ▼
           ┌─────────────────────┐                               ┌──────────────────────┐
           │ NetworkEngine &     │                               │ AppDatabase (Room)   │
           │ Diagnostics Service │                               │ - QuickAccessItemDao │
           │ - PingService       │                               │ - SavedHostDao       │
           │ - PortScanService   │                               │ - CategoryDao        │
           │ - LanScanService    │                               │ - DnsServerDao       │
           │ - DnsService        │                               │ - WolDeviceDao       │
           │ - WhoisService      │                               │ - RssFeedDao         │
           │ - SubnetCalculator  │                               └──────────────────────┘
           └─────────────────────┘
```

---

## 5. Verification & Testing Plan

1. **Compilation Check**: Run `compile_applet` to verify clean build with Kotlin, Room, Compose M3, and networking dependencies.
2. **Quick Access Grid Verification**:
   - Verify initial grid items load correctly.
   - Add a custom external app shortcut (e.g. "Termux" or "Router Web").
   - Test launching external intent / fallback when app not installed.
   - Test editing / deleting custom grid items.
3. **Port Scanner Verification**:
   - Test TCP range scan on known services (e.g. `1-100`, `80-443`).
   - Test UDP scan option with timeout boundaries.
   - Verify filter switching (All, Open, Closed) and progress indicator accuracy.
4. **IP Manager Verification**:
   - Save IP address with custom label, notes, and category.
   - Test real-time search across IP string, label substring, and category chips.
   - Test quick-action launcher directly passing target IP into Port Scanner, Ping, and Traceroute.
5. **Calculators & Discovery**:
   - Test IPv4 / IPv6 calculators edge cases (/0, /32, /128).
   - Test LAN subnet discovery and WoL packet formatting.
