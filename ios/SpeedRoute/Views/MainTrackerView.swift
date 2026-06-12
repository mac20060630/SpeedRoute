import SwiftUI
import MapKit

struct MainTrackerView: View {
    @StateObject private var tripManager = TripManager.shared
    
    // Default region around user
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 20.5937, longitude: 78.9629), // Default to India center
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    
    var body: some View {
        ZStack {
            Map(coordinateRegion: $region, showsUserLocation: true)
                .ignoresSafeArea()
                .onReceive(tripManager.$stats) { stats in
                    if let lat = stats.currentLat, let lng = stats.currentLng, tripManager.stats.isTracking {
                        region.center = CLLocationCoordinate2D(latitude: lat, longitude: lng)
                    }
                }
            
            VStack {
                Spacer()
                
                // Trip Overview Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Current Speed")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("\(String(format: "%.1f", tripManager.stats.currentSpeedKmH)) km/h")
                                .font(.system(size: 32, weight: .bold, design: .rounded))
                        }
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text("Top Speed")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("\(String(format: "%.1f", tripManager.stats.topSpeedKmH)) km/h")
                                .font(.headline)
                        }
                    }
                    
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Distance")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("\(String(format: "%.2f", tripManager.stats.totalDistanceKm)) km")
                                .font(.headline)
                        }
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text("Duration")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("\(tripManager.stats.durationSeconds / 60)m \(tripManager.stats.durationSeconds % 60)s")
                                .font(.headline)
                        }
                    }
                    
                    Button(action: {
                        if tripManager.stats.isTracking {
                            tripManager.stopTracking()
                        } else {
                            tripManager.startTracking()
                        }
                    }) {
                        Text(tripManager.stats.isTracking ? "Stop Tracking" : "Start Tracking")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(tripManager.stats.isTracking ? Color.red : Color.green)
                            .cornerRadius(16)
                            .shadow(radius: 5)
                    }
                }
                .padding()
                .background(Color(.systemBackground).opacity(0.95))
                .cornerRadius(24)
                .shadow(radius: 10)
                .padding()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                NavigationLink(destination: ProfileView()) {
                    Image(systemName: "person.circle.fill")
                        .font(.title2)
                }
            }
        }
    }
}
