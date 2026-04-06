use serde::Serialize;

#[derive(Serialize)]
pub struct AddItemRequest {
    pub uris: Vec<String>,
    pub position: Option<u32>,
}
