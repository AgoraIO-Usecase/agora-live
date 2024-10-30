//
//  GCDTimer.swift
//  GDGK_iOSExam
//
//  Created by cleven on 20.4.21.
//
import UIKit

public class GCDTimer {
    public init() {
        
    }
    public typealias ActionBlock = (String, TimeInterval) -> Void
    private var timerContainer = [String: DispatchSourceTimer]()
    private var currentDuration: TimeInterval = 0

    /// Second timer
    ///
    /// - Parameters:
    /// -name: indicates the name of the timer
    /// -timeInterval: indicates the time interval
    /// -queue: indicates the thread
    /// -repeats: Repeats or not
    /// -action: indicates the action to be performed
    public func scheduledSecondsTimer(withName name: String?,
                               timeInterval: Int,
                               queue: DispatchQueue,
                               action: @escaping ActionBlock)
    {
        currentDuration = TimeInterval(0)
        let scheduledName = name ?? Date().timeString()
        var timer = timerContainer[scheduledName]
        if timer == nil {
            timer = DispatchSource.makeTimerSource(flags: [], queue: queue)
            timer?.resume()
            timerContainer[scheduledName] = timer
        }
        timer?.schedule(deadline: .now(), repeating: .seconds(timeInterval), leeway: .milliseconds(100))
        timer?.setEventHandler(handler: { [weak self] in
            guard let self = self else { return }
            self.currentDuration += TimeInterval(timeInterval)
            action(scheduledName, self.currentDuration)
        })
    }

    /// Second countdown timer
    ///
    /// - Parameters:
    /// -name: indicates the name of the timer
    /// -timeInterval: indicates the time interval
    /// -queue: indicates the thread
    /// -repeats: Repeats or not
    /// -action: indicates the action to be performed
    public func scheduledTimer(withName name: String?,
                        timeInterval: Int,
                        queue: DispatchQueue,
                        action: @escaping ActionBlock)
    {
        let scheduledName = name ?? Date().timeString()
        var timer = timerContainer[scheduledName]
        if timer == nil {
            timer = DispatchSource.makeTimerSource(flags: [], queue: queue)
            timer?.resume()
            timerContainer[scheduledName] = timer
        }
        timer?.schedule(deadline: .now(), repeating: .seconds(timeInterval), leeway: .milliseconds(1000))
        timer?.setEventHandler(handler: {
            action(scheduledName, 1)
        })
    }

    /// Millisecond countdown timer
    /// - Parameters:
    /// -name: indicates the name
    /// -countDown: Countdown milliseconds
    /// -timeInterval: how many milliseconds is the callback time
    /// -queue: indicates the thread
    /// -action: callback
    public func scheduledMillisecondsTimer(withName name: String?,
                                    countDown: TimeInterval,
                                    milliseconds: TimeInterval,
                                    queue: DispatchQueue,
                                    action: @escaping ActionBlock)
    {
        currentDuration = countDown
        let scheduledName = name ?? Date().timeString()
        var timer = timerContainer[scheduledName]
        if timer == nil {
            timer = DispatchSource.makeTimerSource(flags: [], queue: queue)
            timer?.resume()
            timerContainer[scheduledName] = timer
        }
        timer?.schedule(deadline: .now(), repeating: .milliseconds(Int(milliseconds)), leeway: .milliseconds(1))
        timer?.setEventHandler(handler: { [weak self] in
            guard let self = self else { return }
            self.currentDuration -= milliseconds
            action(scheduledName, self.currentDuration)
            if self.currentDuration <= 0 {
                self.destoryTimer(withName: scheduledName)
            }
        })
    }

    /// Destroy the timer named name
    ///
    /// -Parameter name: specifies the name of the timer
    public func destoryTimer(withName name: String?) {
        guard let name = name else { return }
        let timer = timerContainer[name]
        if timer == nil { return }
        timerContainer.removeValue(forKey: name)
        timer?.cancel()
    }

    /// Destroy all timers
    public func destoryAllTimer() {
        timerContainer.forEach {
            destoryTimer(withName: $0.key)
        }
    }

    /// Detects whether a timer named name already exists
    ///
    /// -Parameter name: specifies the name of the timer
    /// -Returns: Returns a bool value
    public func isExistTimer(withName name: String?) -> Bool {
        guard let name = name else { return false }
        return timerContainer[name] != nil
    }
}

extension Date {
    public func timeString(ofStyle style: DateFormatter.Style = .medium) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.timeStyle = style
        dateFormatter.dateStyle = .none
        return dateFormatter.string(from: self)
    }
}
