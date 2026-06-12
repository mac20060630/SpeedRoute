import Foundation

struct Trip: Codable, Identifiable {
    let id: String
    let timestamp: Int64
    let distanceKm: Double
    let durationSeconds: Int
    let topSpeedKmH: Double
    let stoppedTimeSeconds: Int
    let routePoints: [TripPoint]
}

class LocalTripStorage {
    static let shared = LocalTripStorage()
    private let fileManager = FileManager.default
    private let tripsDirectory: URL
    
    private init() {
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        tripsDirectory = documentsDirectory.appendingPathComponent("local_trips")
        
        if !fileManager.fileExists(atPath: tripsDirectory.path) {
            try? fileManager.createDirectory(at: tripsDirectory, withIntermediateDirectories: true, attributes: nil)
        }
    }
    
    func saveTrip(trip: Trip) {
        let fileURL = tripsDirectory.appendingPathComponent("\(trip.id).json")
        do {
            let data = try JSONEncoder().encode(trip)
            try data.write(to: fileURL)
            print("Trip saved locally to \(fileURL.path)")
        } catch {
            print("Failed to save trip: \(error.localizedDescription)")
        }
    }
    
    func getAllTrips() -> [Trip] {
        var trips: [Trip] = []
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: tripsDirectory, includingPropertiesForKeys: nil)
            for url in fileURLs where url.pathExtension == "json" {
                let data = try Data(contentsOf: url)
                let trip = try JSONDecoder().decode(Trip.self, from: data)
                trips.append(trip)
            }
        } catch {
            print("Failed to get trips: \(error.localizedDescription)")
        }
        return trips.sorted(by: { $0.timestamp > $1.timestamp })
    }
    
    func getTrip(id: String) -> Trip? {
        let fileURL = tripsDirectory.appendingPathComponent("\(id).json")
        do {
            let data = try Data(contentsOf: fileURL)
            return try JSONDecoder().decode(Trip.self, from: data)
        } catch {
            print("Trip not found: \(error.localizedDescription)")
            return nil
        }
    }
}
