/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
public class FilterController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    /**
     * Saves a new or updated {@link Filter}.
     *
     * @param filter    The {@link Filter} to save.
     * @param principal The {@link java.security.Principal} of the authenticated user
     * @return The saved {@link Filter}.
     */
    @SuppressWarnings("unused")
    @PutMapping(value = "/filter", produces = JSON)
    @PreAuthorize("@authorizationHelper.maySaveOrDeleteFilter(#filter.getName(), #root)")
    public Filter saveFilter(@RequestBody final Filter filter,
                             Principal principal) {
        filter.setUser(principal.getName());
        return nodeDAO.saveFilter(filter);
    }

    @SuppressWarnings("unused")
    @GetMapping(value = "/filters", produces = JSON)
    public List<Filter> getAllFilters() {
        return nodeDAO.getAllFilters();
    }

    @SuppressWarnings("unused")
    @DeleteMapping(value = "/filter/{name}")
    @PreAuthorize("@authorizationHelper.maySaveOrDeleteFilter(#name, #root)")
    public void deleteFilter(@PathVariable final String name, Principal principal) {
        nodeDAO.deleteFilter(name);
    }
}
