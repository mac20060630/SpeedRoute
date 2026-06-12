import SwiftUI

struct WelcomeView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                VStack {
                    Spacer()
                    
                    Text("How do your Trips Rank?")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    
                    Text("Let's find out.")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.bottom, 32)
                    
                    NavigationLink(destination: Text("Register View")) {
                        Text("Get Started")
                            .font(.headline)
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(Color.white)
                            .cornerRadius(16)
                    }
                    .padding(.horizontal, 24)
                    
                    NavigationLink(destination: Text("Login View")) {
                        Text("Log In")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.white, lineWidth: 2)
                            )
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    
                    Text("By continuing you're accepting our Terms of Use and Privacy Notice")
                        .font(.caption)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.top, 32)
                        .padding(.bottom, 16)
                        .padding(.horizontal, 32)
                }
            }
        }
    }
}

#Preview {
    WelcomeView()
}
