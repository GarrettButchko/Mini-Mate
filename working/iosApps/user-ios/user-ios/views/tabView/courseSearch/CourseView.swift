//
//  GameView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/6/25.
//

import SwiftUI
import MapKit
import shared_user

// MARK: - CourseView

struct CourseView: View {
    @StateObject var courseVM = CourseViewModelSwift()
    @StateObject var viewModel = CourseSearchViewModelSwift()

    var body: some View {
        GeometryReader { geometry in
            if viewModel.hasLocationAccess {
                ZStack {
                    mapView
                    
                    VStack {
                        HStack {
                            Text("Course Search")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                                .padding()
                                .frame(height: 40)
                                .background(Material.regular, in: RoundedRectangle(cornerRadius: 25))
                                .cardShadow()
                            
                            Spacer()
                            
                            LocationButton(action: viewModel.kotlin.recenterMap)
                                .cardShadow()
                        }
                        
                        Spacer()
                        
                        if !viewModel.isSearchPanelVisible {
                            CourseSearchButton()
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                        } else {
                            VStack {
                                if viewModel.selectedMapItem != nil {
                                    CourseResultView()
                                        .transition(.move(edge: .trailing).combined(with: .opacity))
                                } else {
                                    CourseSearchResultsView()
                                        .transition(.move(edge: .leading).combined(with: .opacity))
                                }
                            }
                            .background(Material.regular)
                            .clipShape(RoundedRectangle(cornerRadius: 25))
                            .cardShadow()
                            .frame(height: geometry.size.height * 0.4)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                            .animation(.spring, value: viewModel.selectedMapItem)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 30)
                    .animation(.spring, value: viewModel.isSearchPanelVisible)
                }
            } else {
                VStack {
                    Spacer()
                    Text("Please enable Location Services for this app.\n\nTap 'Open Settings' → Location → Allow While Using the App.")
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 8)
                    Spacer()
                }
                .frame(maxWidth: .infinity)
            }
        }
        .onAppear(perform: viewModel.kotlin.onAppear)
        .environmentObject(courseVM)
        .environmentObject(viewModel)
    }
    
    private var mapView: some View {
        Map(position: $viewModel.mapCameraPosition, selection: $viewModel.selectedMapItem) {
            ForEach(viewModel.mapItems, id: \.self) { item in
                let name = item.name ?? "Unknown"
                let isSupported = viewModel.nameExists[name] ?? false
                
                if #available(iOS 26.0, *) {
                    Marker(name, coordinate: item.location.coordinate)
                        .tint(isSupported ? .purple : .green)
                } else {
                    Marker(name, coordinate: item.placemark.coordinate)
                        .tint(isSupported ? .purple : .green)
                }
            }
            UserAnnotation()
        }
        .mapControls {
            MapCompass().mapControlVisibility(.hidden)
        }
        .animation(.spring, value: viewModel.mapCameraPosition)
    }
}

private struct LocationButton: View {
    var action: () -> Void
    
    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Material.regular)
                    .frame(width: 40, height: 40)
                
                Image(systemName: "location.fill")
                    .resizable()
                    .foregroundColor(.primary)
                    .frame(width: 20, height: 20)
            }
        }
    }
}

// MARK: - Placeholder View Modifier
// This is a placeholder for your custom .cardShadow() modifier
private struct CardShadow: ViewModifier {
    func body(content: Content) -> some View {
        content.shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
    }
}

private extension View {
    func cardShadow() -> some View {
        self.modifier(CardShadow())
    }
}

