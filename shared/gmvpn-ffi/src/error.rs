//! UniFFI error surface. Closed set mirroring `gmvpn_core::Error`.

use gmvpn_core::Error;

/// Error variants returned across the FFI boundary.
///
/// Every variant carries a textual `message` so the platform side can
/// surface the precise reason without reaching into the enum shape.
#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum GmvpnError {
    #[error("invalid URI: {message}")]
    InvalidUri { message: String },

    #[error("unsupported protocol: {message}")]
    UnsupportedProtocol { message: String },

    #[error("missing required field: {message}")]
    MissingField { message: String },

    #[error("invalid value for field '{field}': {reason}")]
    InvalidValue { field: String, reason: String },

    #[error("decode error: {message}")]
    Decode { message: String },

    #[error("serialization error: {message}")]
    Serialization { message: String },
}

impl From<Error> for GmvpnError {
    fn from(value: Error) -> Self {
        match value {
            Error::InvalidUri(message) => Self::InvalidUri { message },
            Error::UnsupportedProtocol(message) => Self::UnsupportedProtocol { message },
            Error::MissingField(field) => Self::MissingField {
                message: field.to_string(),
            },
            Error::InvalidValue { field, reason } => Self::InvalidValue {
                field: field.to_string(),
                reason,
            },
            Error::Decode(message) => Self::Decode { message },
            Error::Serialization(message) => Self::Serialization { message },
        }
    }
}
