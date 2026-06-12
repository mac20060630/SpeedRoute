import Foundation
import CoreLocation
import CoreMotion

struct TripPoint: Codable {
    let latitude: Double
    let longitude: Double
}

struct TripStats {
    var isTracking: Bool = false
    var currentSpeedKmH: Double = 0.0
    var topSpeedKmH: Double = 0.0
    var totalDistanceKm: Double = 0.0
    var durationSeconds: Int = 0
    var stoppedTimeSeconds: Int = 0
    var routePoints: [TripPoint] = []
    
    // Persistent stats
    var totalTrips: Int = 0
    var allTimeDistanceKm: Double = 0.0
}

class TripManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = TripManager()
    
    @Published var stats = TripStats()
    
    private let locationManager = CLLocationManager()
    private let motionManager = CMMotionManager()
    
    private var startTime: Date?
    private var lastLocation: CLLocation?
    private var timer: Timer?
    
    override private init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.distanceFilter = 5.0 // Updates every 5 meters
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        
        loadPersistentStats()
    }
    
    func requestPermissions() {
        locationManager.requestAlwaysAuthorization()
    }
    
    func startTracking() {
        requestPermissions()
        startTime = Date()
        lastLocation = nil
        
        stats.isTracking = true
        stats.currentSpeedKmH = 0.0
        stats.topSpeedKmH = 0.0
        stats.totalDistanceKm = 0.0
        stats.durationSeconds = 0
        stats.stoppedTimeSeconds = 0
        stats.routePoints = []
        
        locationManager.startUpdatingLocation()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateDuration()
        }
    }
    
    func stopTracking() {
        stats.isTracking = false
        locationManager.stopUpdatingLocation()
        timer?.invalidate()
        timer = nil
        
        // Save to persistent
        stats.totalTrips += 1
        savePersistentStats()
        
        // TODO: Save local trip to filesystem
    }
    
    private func updateDuration() {
        guard let start = startTime else { return }
        stats.durationSeconds = Int(Date().timeIntervalSince(start))
        
        if stats.currentSpeedKmH < 1.0 {
            stats.stoppedTimeSeconds += 1
        }
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        // Filter inaccurate points
        if location.horizontalAccuracy > 50 { return }
        
        let speedKmH = max(location.speed * 3.6, 0)
        stats.currentSpeedKmH = speedKmH
        
        if speedKmH > stats.topSpeedKmH {
            stats.topSpeedKmH = speedKmH
        }
        
        if let last = lastLocation {
            let distance = location.distance(from: last) / 1000.0 // in km
            stats.totalDistanceKm += distance
            stats.allTimeDistanceKm += distance
        }
        
        // Add route point
        stats.routePoints.append(TripPoint(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude))
        
        lastLocation = location
    }
    
    // MARK: - Persistence
    
    private func loadPersistentStats() {
        stats.totalTrips = UserDefaults.standard.integer(forKey: "totalTrips")
        stats.allTimeDistanceKm = UserDefaults.standard.double(forKey: "allTimeDistanceKm")
    }
    
    private func savePersistentStats() {
        UserDefaults.standard.set(stats.totalTrips, forKey: "totalTrips")
        UserDefaults.standard.set(stats.allTimeDistanceKm, forKey: "allTimeDistanceKm")
    }
}
