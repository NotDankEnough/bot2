// Copyright (C) 2022 NotDankEnough (ilotterytea)
// 
// This file is part of itb2.
// 
// itb2 is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// itb2 is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with itb2.  If not, see <http://www.gnu.org/licenses/>.

import IArguments from "./IArguments";

namespace IModule {
    export interface IModule {
        cooldownMs?: number | undefined;
        permissions?: Permissions | undefined;
        minPermissions?: Permissions | undefined;
        run: (Arguments: IArguments, ...args: []) => Promise<string | boolean>;
    }
    export type Permissions = "public" |
    "vip" |
    "mod" |
    "broadcaster" |
    "supauser";
}

export default IModule;