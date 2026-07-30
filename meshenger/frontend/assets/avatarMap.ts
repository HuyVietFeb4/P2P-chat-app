/**
 * avatarMap.ts
 *
 * Single source of truth for all avatar assets.
 *
 * Usage:
 *   import { getAvatarSource, AVATAR_LIST, DEFAULT_AVATAR } from '@/assets/avatarMap';
 *
 *   // Resolve an avatar ID to an ImageSource (for <Image source={...} />)
 *   const src = getAvatarSource('avt3');          // → PNG require()
 *   const src = getAvatarSource(null);             // → DEFAULT_AVATAR (avatar.png)
 *
 *   // Iterate the full list (e.g. for a selection grid — excludes avt0)
 *   AVATAR_LIST.filter(a => a.id !== 'avt0').map(...)
 */

import { ImageSourcePropType } from 'react-native';

/** Fallback shown when no avatar has been selected yet. */
export const DEFAULT_AVATAR: ImageSourcePropType = require('./avt_set/global.png');

/** Keyed lookup: avatarId → require'd PNG. */
const AVATAR_SOURCES: Record<string, ImageSourcePropType> = {
    avt0:  require('./avt_set/avt0.png'),
    avt1:  require('./avt_set/avt1.png'),
    avt2:  require('./avt_set/avt2.png'),
    avt3:  require('./avt_set/avt3.png'),
    avt4:  require('./avt_set/avt4.png'),
    avt5:  require('./avt_set/avt5.png'),
    avt6:  require('./avt_set/avt6.png'),
    avt7:  require('./avt_set/avt7.png'),
    avt8:  require('./avt_set/avt8.png'),
    avt9:  require('./avt_set/avt9.png'),
    avt10: require('./avt_set/avt10.png'),
    avt11: require('./avt_set/avt11.png'),
    avt12: require('./avt_set/avt12.png'),
};

/**
 * Resolve an avatar ID string to the matching ImageSource.
 * Returns DEFAULT_AVATAR when the ID is null, undefined, empty, or unknown.
 */
export function getAvatarSource(avatarId: string | null | undefined): ImageSourcePropType {
    if (!avatarId) return DEFAULT_AVATAR;
    return AVATAR_SOURCES[avatarId] ?? DEFAULT_AVATAR;
}

/** Ordered list of all avatars — convenient for selection grids. */
export const AVATAR_LIST: Array<{ id: string; source: ImageSourcePropType }> = Object.entries(
    AVATAR_SOURCES,
).map(([id, source]) => ({ id, source }));
