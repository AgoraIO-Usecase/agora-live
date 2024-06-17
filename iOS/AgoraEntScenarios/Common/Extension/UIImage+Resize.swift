
import UIKit

extension UIImage {
    // Image compression
    func resetSizeOfImageData(maxSize: NSInteger) -> Data? {
        let sourceImage = self
        // First determine whether the current quality meets the requirements, and if not, proceed with compression
        var finalImageData = sourceImage.jpegData(compressionQuality: 1.0)
        guard let sizeOrigin = finalImageData?.count else {
            return nil
        }
        let sizeOriginKB = sizeOrigin  / 1000
        if sizeOriginKB <= maxSize {
            return finalImageData
        }
        // Obtain the original image aspect ratio
        let sourceImageAepectRatio = sourceImage.size.width / sourceImage.size.height
        // Adjust the resolution first
        var defaultSize = CGSize(width: 1024, height: 1024 / sourceImageAepectRatio)
        guard let newImage = newSizeImage(defaultSize, sourceImage: sourceImage) else { return nil }
        finalImageData = newImage.jpegData(compressionQuality: 1.0)
        
        // Save compression factor
        var compressionQualityArr: [CGFloat] = []
        let avg: CGFloat = 1.0 / 250
        var value = avg
        for i in (1...250).reversed() {
            value = CGFloat(i) * avg
            compressionQualityArr.append(value)
        }
        /*
         调整大小
         说明：压缩系数数组compressionQualityArr是从大到小存储。
         */
        // Approach: Use binary search
        guard let imageData = halfFunction(compressionQualityArr, image: newImage, finalImageData: finalImageData, maxSize: maxSize) else {
            return nil
        }
        finalImageData = imageData
        // If it still fails to compress to the specified size, perform a resolution reduction
        while imageData.count == 0 {
            // Reduce resolution by 100 each time
            let reduceW: CGFloat = 100.0
            let reduceH = 100.0 / sourceImageAepectRatio
            if defaultSize.width - reduceW <= 0 || defaultSize.height - reduceH <= 0 {
                break
            }
            defaultSize = CGSize(width: defaultSize.width - reduceW, height: defaultSize.height - reduceH)
            guard let last = compressionQualityArr.last,
                let imgData = newImage.jpegData(compressionQuality: last),
                let img = UIImage(data: imgData) else {
                break
            }
            
            let image = newSizeImage(defaultSize, sourceImage: img)
            finalImageData = halfFunction(compressionQualityArr, image: image, finalImageData: image?.jpegData(compressionQuality: 1.0), maxSize: maxSize)
            guard let sizeOrigin = finalImageData?.count else {
                break
            }
            let sizeOriginKB = sizeOrigin  / 1000
            if sizeOriginKB <= maxSize {
                break
            }
        }
        return finalImageData
    }

    // Adjust image resolution/size (proportional scaling)
    private func newSizeImage(_ size: CGSize, sourceImage: UIImage) -> UIImage? {
        var newSize = CGSize(width: sourceImage.size.width, height: sourceImage.size.height)
        let tempH = newSize.height / size.height
        let tempW = newSize.width / size.width
        if tempW > 1.0, tempW > tempH {
            newSize = CGSize(width: sourceImage.size.width / tempW, height: sourceImage.size.height / tempW)
        } else if tempH > 1.0, tempW < tempH {
            newSize = CGSize(width: sourceImage.size.width / tempH, height: sourceImage.size.height / tempH)
        }
        UIGraphicsBeginImageContext(newSize)
        sourceImage.draw(in: CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height))
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return newImage
    }

    private func halfFunction(_ arr: [CGFloat], image: UIImage?, finalImageData: Data?, maxSize: NSInteger) -> Data? {
        var tempData = Data.init()
        var start = 0
        var end = arr.count - 1
        var index = 0
        var difference = NSInteger.max
        while start <= end {
            index = start + (end - start) / 2
            if let img = image, let finalImageData = img.jpegData(compressionQuality: arr[index]) {
                let sizeOrigin = finalImageData.count
                let sizeOriginKB = sizeOrigin / 1024
// print("Currently downgraded to quality \(sizeOriginKB)")
// print("\ nstart: \(start) \ nend: \(end) \ nindex: \ (index) \ ncompression factor: \ (String (format:"% f ", arr [index])")
                if sizeOriginKB > maxSize {
                    start = index + 1
                } else if sizeOriginKB < maxSize {
                    if maxSize - sizeOriginKB < difference {
                        difference = maxSize - sizeOriginKB
                        tempData = finalImageData
                    }
                    if index <= 0 {
                        break
                    }
                    end = index - 1
                }
            } else {
                break
            }
        }
        return tempData
    }
}
