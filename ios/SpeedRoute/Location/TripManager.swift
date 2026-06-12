import Foundation
import CoreLocation
import CoreMotion

struct TripPoint: Codable {
    let latitude: Double
    let longitude: Double
    let speedKmH: Double
}

struct TripStats {
    var isTracking: Bool = false
    var currentSpeedKmH: Double = 0.0
    var topSpeedKmH: Double = 0.0
    var totalDistanceKm: Double = 0.0
    var durationSeconds: Int = 0
    var stoppedTimeSeconds: Int = 0
    
    var maxAcceleration: Double = 0.0
    var maxDeceleration: Double = 0.0
    var peakGForce: Double = 0.0
    var topCornerSpeedKmH: Double = 0.0
    
    var totalStops: Int = 0
    var best0To100TimeSec: Double? = nil
    var leftTurns: Int = 0
    var rightTurns: Int = 0
    var brakeEvents: Int = 0
    var laneChanges: Int = 0
    
    var currentLat: Double? = nil
    var currentLng: Double? = nil
    var currentAltitude: Double = 0.0
    var routePoints: [TripPoint] = []
    
    // Persistent stats
    var totalTrips: Int = 0
    var totalAllTimeDurationSeconds: Int = 0
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
    
    private var speed0StartTime: Date?
    private var isCurrentlyStopped = true
    private var previousSpeedKmH: Double = 0.0
    
    override private init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.distanceFilter = 5.0
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
        speed0StartTime = nil
        isCurrentlyStopped = true
        previousSpeedKmH = 0.0
        
        stats.isTracking = true
        stats.currentSpeedKmH = 0.0
        stats.topSpeedKmH = 0.0
        stats.totalDistanceKm = 0.0
        stats.durationSeconds = 0
        stats.stoppedTimeSeconds = 0
        stats.maxAcceleration = 0.0
        stats.maxDeceleration = 0.0
        stats.peakGForce = 0.0
        stats.topCornerSpeedKmH = 0.0
        stats.totalStops = 0
        stats.leftTurns = 0
        stats.rightTurns = 0
        stats.brakeEvents = 0
        stats.laneChanges = 0
        stats.routePoints = []
        
        locationManager.startUpdatingLocation()
        startMotionTracking()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateDuration()
        }
    }
    
    func stopTracking() {
        stats.isTracking = false
        locationManager.stopUpdatingLocation()
        motionManager.stopDeviceMotionUpdates()
        timer?.invalidate()
        timer = nil
        
        stats.totalTrips += 1
        stats.totalAllTimeDurationSeconds += stats.durationSeconds
        savePersistentStats()
    }
    
    private func updateDuration() {
        guard let start = startTime else { return }
        stats.durationSeconds = Int(Date().timeIntervalSince(start))
    }
    
    private func startMotionTracking() {
        guard motionManager.isDeviceMotionAvailable else { return }
        motionManager.deviceMotionUpdateInterval = 0.1
        motionManager.startDeviceMotionUpdates(to: .main) { [weak self] motion, error in
            guard let self = self, let motion = motion, error == nil else { return }
            
            let accelX = motion.userAcceleration.x
            let accelY = motion.userAcceleration.y
            let accelZ = motion.userAcceleration.z
            
            let gForce = sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ)
            if gForce > self.stats.peakGForce {
                self.stats.peakGForce = gForce
            }
            
            let forwardAccel = -accelY * 9.81
            if forwardAccel > self.stats.maxAcceleration {
                self.stats.maxAcceleration = forwardAccel
            } else if forwardAccel < -2.0 {
                if abs(forwardAccel) > self.stats.maxDeceleration {
                    self.stats.maxDeceleration = abs(forwardAccel)
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        stats.currentLat = location.coordinate.latitude
        stats.currentLng = location.coordinate.longitude
        stats.currentAltitude = location.altitude
        
        if !stats.isTracking { return }
        if location.horizontalAccuracy > 50 { return }
        
        let speedKmH = max(location.speed * 3.6, 0)
        stats.currentSpeedKmH = speedKmH
        
        if speedKmH > stats.topSpeedKmH {
            stats.topSpeedKmH = speedKmH
        }
        
        if speedKmH < 1.0 {
            if !isCurrentlyStopped {
                isCurrentlyStopped = true
                stats.totalStops += 1
            }
            if let lastTime = lastLocation?.timestamp {
                stats.stoppedTimeSeconds += Int(location.timestamp.timeIntervalSince(lastTime))
            }
            speed0StartTime = Date()
        } else {
            isCurrentlyStopped = false
        }
        
        if speedKmH >= 100.0, let s0Time = speed0StartTime {
            let timeTo100 = Date().timeIntervalSince(s0Time)
            if timeTo100 > 0.5 {
                if stats.best0To100TimeSec == nil || timeTo100 < stats.best0To100TimeSec! {
                    stats.best0To100TimeSec = timeTo100
                }
            }
            speed0StartTime = nil
        }
        
        if let last = lastLocation {
            let distance = location.distance(from: last) / 1000.0
            if distance < 0.5 {
                stats.totalDistanceKm += distance
                stats.allTimeDistanceKm += distance
            }
        }
        
        stats.routePoints.append(TripPoint(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude, speedKmH: speedKmH))
        
        previousSpeedKmH = speedKmH
        lastLocation = location
    }
    
    private func loadPersistentStats() {
        stats.totalTrips = UserDefaults.standard.integer(forKey: "totalTrips")
        stats.totalAllTimeDurationSeconds = UserDefaults.standard.integer(forKey: "totalAllTimeDuration")
        stats.allTimeDistanceKm = UserDefaults.standard.double(forKey: "allTimeDistanceKm")
        if UserDefaults.standard.object(forKey: "best0100") != nil {
            stats.best0To100TimeSec = UserDefaults.standard.double(forKey: "best0100")
        }
    }
    
    private func savePersistentStats() {
        UserDefaults.standard.set(stats.totalTrips, forKey: "totalTrips")
        UserDefaults.standard.set(stats.totalAllTimeDurationSeconds, forKey: "totalAllTimeDuration")
        UserDefaults.standard.set(stats.allTimeDistanceKm, forKey: "allTimeDistanceKm")
        if let best = stats.best0To100TimeSec {
            UserDefaults.standard.set(best, forKey: "best0100")
        }
    }
}
