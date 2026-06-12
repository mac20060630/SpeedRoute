import SwiftUI
import FirebaseAuth

struct ProfileView: View {
    @State private var selectedVehicle = "Car"
    @State private var updateAvailable = false
    @State private var latestVersion = "1.0"
    @State private var showingUpdateAlert = false
    
    let vehicles = ["Car", "Bike"]
    
    var body: some View {
        Form {
            Section(header: Text("Vehicle Profile")) {
                Picker("Vehicle Type", selection: $selectedVehicle) {
                    ForEach(vehicles, id: \.self) {
                        Text($0)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .onChange(of: selectedVehicle) { newValue in
                    UserDefaults.standard.set(newValue, forKey: "vehicleType")
                }
            }
            
            Section(header: Text("Account")) {
                if let user = Auth.auth().currentUser {
                    Text("Logged in as: \(user.email ?? "Unknown")")
                    Button("Sign Out") {
                        try? Auth.auth().signOut()
                    }
                    .foregroundColor(.red)
                } else {
                    Text("Not logged in")
                }
            }
        }
        .navigationTitle("Profile")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    showingUpdateAlert = true
                }) {
                    ZStack {
                        Image(systemName: "bell.fill")
                            .foregroundColor(.primary)
                        if updateAvailable {
                            Circle()
                                .fill(Color.red)
                                .frame(width: 10, height: 10)
                                .offset(x: 10, y: -10)
                        }
                    }
                }
                .alert(isPresented: $showingUpdateAlert) {
                    if updateAvailable {
                        return Alert(
                            title: Text("Update Available"),
                            message: Text("Version \(latestVersion) is available!"),
                            primaryButton: .default(Text("Download")),
                            secondaryButton: .cancel()
                        )
                    } else {
                        return Alert(
                            title: Text("Up to date"),
                            message: Text("You have the latest version."),
                            dismissButton: .default(Text("OK"))
                        )
                    }
                }
            }
        }
        .onAppear {
            selectedVehicle = UserDefaults.standard.string(forKey: "vehicleType") ?? "Car"
            // Start checking for updates
            AppUpdater.shared.checkForUpdates { available, version in
                self.updateAvailable = available
                self.latestVersion = version ?? "1.0"
            }
        }
    }
}
