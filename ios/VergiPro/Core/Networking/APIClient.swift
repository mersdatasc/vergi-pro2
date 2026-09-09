import Foundation

actor APIClient {
    typealias AccessTokenProvider = @Sendable () async throws -> String?

    private let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let tokenProvider: AccessTokenProvider

    init(
        baseURL: URL = URL(string: "https://api.vergi.pro/api/")!,
        session: URLSession = .shared,
        tokenProvider: @escaping AccessTokenProvider = { SessionController.shared.currentAccessToken() }
    ) {
        self.baseURL = baseURL
        self.session = session
        self.tokenProvider = tokenProvider
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder
    }

    func send<Response: Decodable>(
        _ endpoint: APIEndpoint<Response>,
        organization: OrganizationContext? = nil,
        idempotencyKey: UUID? = nil
    ) async throws -> Response {
        let cleanPath = endpoint.path.hasPrefix("/") ? String(endpoint.path.dropFirst()) : endpoint.path
        let url = baseURL.appendingPathComponent(cleanPath)
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        components?.queryItems = endpoint.query.isEmpty ? nil : endpoint.query
        guard let finalURL = components?.url else { throw APIError.invalidRequest }

        var request = URLRequest(url: finalURL, timeoutInterval: 30)
        request.httpMethod = endpoint.method.rawValue
        request.httpBody = endpoint.body
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        if endpoint.body != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if let organization { request.setValue(String(organization.organizationID), forHTTPHeaderField: "X-Organization-ID") }
        if let idempotencyKey { request.setValue(idempotencyKey.uuidString, forHTTPHeaderField: "Idempotency-Key") }
        if let token = try await tokenProvider() { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError where [.notConnectedToInternet, .networkConnectionLost, .timedOut].contains(error.code) {
            throw APIError.networkUnavailable
        }

        guard let http = response as? HTTPURLResponse else { throw APIError.server(requestID: nil) }
        guard (200..<300).contains(http.statusCode) else { throw mapError(status: http.statusCode, data: data, headers: http.allHeaderFields) }
        do { return try decoder.decode(Response.self, from: data) }
        catch { throw APIError.decoding }
    }

    func uploadMultipart<Response: Decodable>(
        path: String,
        fileData: Data,
        filename: String,
        formFields: [String: String] = [:],
        organization: OrganizationContext? = nil
    ) async throws -> Response {
        return try await uploadMultipartBatch(
            path: path,
            files: [(data: fileData, filename: filename, formKey: "file")],
            formFields: formFields,
            organization: organization
        )
    }

    func uploadMultipartBatch<Response: Decodable>(
        path: String,
        files: [(data: Data, filename: String, formKey: String)],
        formFields: [String: String] = [:],
        organization: OrganizationContext? = nil
    ) async throws -> Response {
        let cleanPath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        let url = baseURL.appendingPathComponent(cleanPath)

        let boundary = "===VergiProBoundary\(UUID().uuidString.replacingOccurrences(of: "-", with: ""))==="
        var request = URLRequest(url: url, timeoutInterval: 60)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        if let organization { request.setValue(String(organization.organizationID), forHTTPHeaderField: "X-Organization-ID") }
        if let token = try await tokenProvider() { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }

        var body = Data()
        let crlf = "\r\n"

        // Text fields
        for (key, value) in formFields {
            body.append("--\(boundary)\(crlf)".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(key)\"\(crlf)\(crlf)".data(using: .utf8)!)
            body.append("\(value)\(crlf)".data(using: .utf8)!)
        }

        // Files
        for item in files {
            body.append("--\(boundary)\(crlf)".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(item.formKey)\"; filename=\"\(item.filename)\"\(crlf)".data(using: .utf8)!)
            let contentType = item.filename.hasSuffix(".pdf") ? "application/pdf" : "image/jpeg"
            body.append("Content-Type: \(contentType)\(crlf)\(crlf)".data(using: .utf8)!)
            body.append(item.data)
            body.append(crlf.data(using: .utf8)!)
        }

        // End
        body.append("--\(boundary)--\(crlf)".data(using: .utf8)!)
        request.httpBody = body

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIError.networkUnavailable
        }

        guard let http = response as? HTTPURLResponse else { throw APIError.server(requestID: nil) }
        guard (200..<300).contains(http.statusCode) else { throw mapError(status: http.statusCode, data: data, headers: http.allHeaderFields) }
        do { return try decoder.decode(Response.self, from: data) }
        catch { throw APIError.decoding }
    }

    private func mapError(status: Int, data: Data, headers: [AnyHashable: Any]) -> APIError {
        let envelope = try? decoder.decode(APIErrorEnvelope.self, from: data)
        let requestID = envelope?.requestID
        return switch status {
        case 401: .authenticationRequired
        case 403: .forbidden
        case 409: .conflict
        case 422: .validation(requestID: requestID)
        case 429: .rateLimited(retryAfter: envelope?.retryAfter)
        case 503: .maintenance
        default: .server(requestID: requestID)
        }
    }
}
