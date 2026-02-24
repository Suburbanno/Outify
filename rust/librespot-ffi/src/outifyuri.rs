use crate::session::get_username;

#[derive(Clone, PartialEq, Eq, Hash)]
pub enum OutifyUri {
    Liked,
}

impl OutifyUri {
    pub fn to_uri(&self) -> String {
        match &self {
            OutifyUri::Liked => {
                let user_id = get_username();
                format!("spotify:user:{}:collection", user_id)
            }
        }
    }
}
