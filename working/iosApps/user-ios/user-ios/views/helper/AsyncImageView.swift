//
//  AsyncImageView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 6/13/25.
//

import SwiftUI

struct AsyncImageView: View {
    
    var image: String?
    
    var height: CGFloat? = 60
    
    var body: some View{
        if let image = image {
            AsyncImage(url: URL(string: image)) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                        .frame(height: height)
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFit()
                        .frame(height: height)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .clipped()
                case .failure:
                    Image(systemName: "photo")
                        .resizable()
                        .scaledToFit()
                        .frame(height: height)
                        .foregroundColor(.gray)
                        .background(Color.gray.opacity(0.2))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                @unknown default:
                    EmptyView()
                }
            }
        }
    }
}
