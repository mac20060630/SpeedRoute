import Foundation
import FirebaseFirestore

class AppUpdater {
    static let shared = AppUpdater()
    private let db = Firestore.firestore()
    
    let currentVersion = "1.0"
    
    func checkForUpdates(completion: @escaping (Bool, String?) -> Void) {
        db.collection("app_metadata").document("version").getDocument { snapshot, error in
            guard let data = snapshot?.data(), error == nil else {
                completion(false, nil)
                return
            }
            
            if let latestVersion = data["latest_version"] as? String {
                if latestVersion.compare(self.currentVersion, options: .numeric) == .orderedDescending {
                    completion(true, latestVersion)
                } else {
                    completion(false, latestVersion)
                }
            } else {
                completion(false, nil)
            }
        }
    }
}
